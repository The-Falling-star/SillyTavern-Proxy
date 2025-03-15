package com.ling.sillytavernproxy.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ling.sillytavernproxy.config.FinalNumber;
import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.DeepSeek.DeepSeekRequestBody;
import com.ling.sillytavernproxy.entity.DeepSeek.PowChallenge;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.properties.DeepSeekProperty;
import com.ling.sillytavernproxy.util.DeepSeekHashV1;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("deepSeekService")
public class DeepSeekService implements DialogService {

    private static int tokenCount;

    private final WebClient webClient;

    private final List<String> tokens;

    ReactiveRedisTemplate<String, String> redisTemplate;

    RestTemplate restTemplate;

    public DeepSeekService(ReactiveRedisTemplate<String, String> redisTemplate, DeepSeekProperty deepSeekProperty, RestTemplate restTemplate) {
        webClient = WebClient.builder().baseUrl(FinalNumber.DEEP_SEEK_BASE_URL).build();
        tokenCount = 0;
        this.redisTemplate = redisTemplate;
        tokens = deepSeekProperty.getTokens() == null ? new ArrayList<>() : deepSeekProperty.getTokens();
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, Object> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        DeepSeekRequestBody requestBody = new DeepSeekRequestBody();
        // 会话id在doOnBefore设置,因为多次请求的话会话id是不一样的
        requestBody.setPrompt(/*JSONUtil.toJsonStr(dialogInputDTO.getMessages())*/"你好啊");
        return BeanUtil.beanToMap(requestBody, true, false);
    }

    /*
    {
        "choices": [
            {
                "index": 0,
                "delta": {
                    "content": "现在",
                    "type": "thinking"
                }
            }
        ],
        "model": "",
        "chunk_token_usage": 1,
        "created": 1741872935,
        "message_id": 8,
        "parent_id": 7
    }
     */
    @Override
    public DialogVO streamResponseToDialogVO(Integer index, String data) {
        String json = data.split("data:")[0].strip();
        JSONObject jsonObject = JSONUtil.parseObj(json);
        List<?> choices = jsonObject.get("choices", List.class);
        if (!choices.isEmpty()) return null;
        JSONObject result = JSONUtil.parseObj(choices.getFirst());
        String content = result.get("delta", JSONObject.class).get("content", String.class);
        Message message = new Message("assistant", content);
        return new DialogVO(List.of(new CommonReplyVO(message, index, false)));
    }

    @Override
    public DialogVO notStreamResponseToDialogVO(Integer index, String data) {
        return streamResponseToDialogVO(index, data);
    }

    @Override
    public String getUrl() {
        return "https://chat.deepseek.com/api/v0/chat/completion";
    }

    @Override
    public WebClient getWebClient() {
        return webClient;
    }

    /**
     * 获取一个会话id
     *
     * @return 会话id字符串
     */
    private Flux<String> createSessionId() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("character_id", null);
        return webClient.post()
                .uri("/chat_session/create")
                .bodyValue(requestBody)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, tokens.get(tokenCount))
                .retrieve()
                .bodyToFlux(JSONObject.class)
                .map(response -> response.get("data", JSONObject.class)
                        .get("biz_data", JSONObject.class)
                        .get("id", String.class));
    }

    @Override
    public Flux<HttpHeaders> getHttpHeaders() {
        log.info("开始获取请求头");
        String key = "DeepSeek:" + tokens.get(tokenCount);
        HttpHeaders headers = new HttpHeaders(); // 创建放外面
        headers.set(HttpHeaders.AUTHORIZATION, tokens.get(tokenCount)); // 提前设置 Authorization

        return redisTemplate.opsForHash()
                .get(key, FinalNumber.DEEPSEEK_CHALLENGE_EXPIRE)
                .switchIfEmpty(Mono.just(""))
                .flatMap(expireTime -> {
                    if (expireTime.toString().isEmpty() || Instant.ofEpochMilli(Long.parseLong((String) expireTime)).isBefore(Instant.now())) {
                        log.info("Pow值过期了或者根本没计算Pow值");
                        // 创建 challenge
                        PowChallenge challenge = createChallenge();

                        // 计算挑战
                        String header = DeepSeekHashV1.computePowAnswer(challenge);
                        if (header == null || header.isEmpty()) {
                            return Mono.error(new RuntimeException("计算Pow失败"));
                        }
                        log.info("计算出请求头为:{}", header);

                        // 将 challenge 和 header 存入 Redis (两个操作合并为一个)
                        Map<Object, Object> entries = new HashMap<>();
                        entries.put(FinalNumber.DEEPSEEK_CHALLENGE_EXPIRE, String.valueOf(challenge.getExpireAt()));
                        entries.put(FinalNumber.DEEP_SEEK_CHALLENGE_HEADER, header);

                        return redisTemplate.opsForHash()
                                .putAll(key, entries)
                                .doOnNext(success -> {
                                    if (success) log.info("成功将请求头和过期时间存入redis");
                                    else log.info("将请求头和过期时间存入redis失败了");
                                })
                                .thenReturn(header);
                    } else return redisTemplate.opsForHash()
                            .get(key, FinalNumber.DEEP_SEEK_CHALLENGE_HEADER)
                            .map(header -> {
                                String header1 = (String) header;
                                log.info("成功从redis中加载请求头{}", header1);
                                return header1;
                            });
                })
                .map(header -> {
                    // 设置 header
                    headers.set(FinalNumber.DEEP_SEEK_CHALLENGE_HEADER, header);
                    log.info("获取请求头成功:{}", header);
                    return headers;
                })
                .flux();
    }

    /**
     * 像deepseek申请一个Pow挑战参数
     *
     * @return PowChallenge挑战参数实体类
     */
    private PowChallenge createChallenge() {
        return webClient.post()
                .uri("/chat/create_pow_challenge")
                .bodyValue(Map.of("target_path", FinalNumber.DEEPSEEK_TARGET_PATH))
                .header(HttpHeaders.AUTHORIZATION, tokens.get(tokenCount))
                .retrieve()
                .bodyToMono(JSONObject.class)
                .map(response -> response.get("data",JSONObject.class)
                        .get("biz_data",JSONObject.class)
                        .get("challenge", PowChallenge.class))
                .block();
    }

    /**
     * 回复完后删除会话
     */
    @Override
    public void doOnComplete(Integer index) {
        /*if(index == null) return;
        // 从redis获取会话id
        String key = "DeepSeek:" + tokens.get(tokenCount);
        String sessionId = (String) redisTemplate.opsForHash().get(key, FinalNumber.DEEPSEEK_SESSION_ID + index);

        // 删除会话id
        webClient.post()
                .uri("chat_session/delete")
                .bodyValue(Map.of("chat_session_id", sessionId))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), res -> {
                    log.info("删除对话失败");
                    return null;
                });
        // 从redis删除会话id
        redisTemplate.opsForHash().delete(key, FinalNumber.DEEPSEEK_SESSION_ID + index);*/
    }

    @Override
    public Flux<Map<String, Object>> doOnBefore(Map<String, Object> requestBody, Integer index) {
        // 创建会话id
        return createSessionId()
                .flatMap(sessionId -> {
                    log.info("成功创建会话id:{}", sessionId);
                    String key = "DeepSeek:" + tokens.get(tokenCount);
                    requestBody.put("chat_session_id", sessionId);
                    return redisTemplate.opsForHash()
                            .put(key, FinalNumber.DEEPSEEK_SESSION_ID + index, sessionId)
                            .doOnNext(success -> {
                                if (success) log.info("成功将会话id存入redis");
                                else log.info("将会话id存入redis失败了");
                            })
                            .thenReturn(requestBody);
                });
    }

    @Override
    public void sendOnBefore(DialogInputDTO dialogInputDTO) {
        tokenCount = ++tokenCount % tokens.size();
    }
}
