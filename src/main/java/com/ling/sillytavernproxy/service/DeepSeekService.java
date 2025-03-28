package com.ling.sillytavernproxy.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ling.sillytavernproxy.config.FinalNumber;
import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.DeepSeek.DeepSeekRequestBody;
import com.ling.sillytavernproxy.entity.DeepSeek.PowChallenge;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.exception.TokenExpireException;
import com.ling.sillytavernproxy.properties.DeepSeekProperty;
import com.ling.sillytavernproxy.util.DeepSeekHashV1;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import com.ling.sillytavernproxy.vo.reply.ReplyVO;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public DeepSeekService(ReactiveRedisTemplate<String, String> redisTemplate, DeepSeekProperty deepSeekProperty) {
        webClient = WebClient.builder()
                .baseUrl(FinalNumber.DEEP_SEEK_BASE_URL)
                .defaultHeader("X-App-Version", "20241129.1")
                .defaultHeader("X-Client-Locale", "zh_CN")
                .defaultHeader("X-Client-Platform", "web")
                .defaultHeader("X-Client-Version", "1.0.0-always")
                .defaultHeader(HttpHeaders.ORIGIN, "https://chat.deepseek.com")
                .defaultHeader(HttpHeaders.PRAGMA, "no-cache")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0")
                .defaultHeader("sec-ch-ua-mobile", "?0")
                .defaultHeader("sec-fetch-site", "same-origin")
                .defaultHeader("sec-fetch-mode", "cors")
                .defaultHeader("sec-fetch-platform", "windows")
                .defaultHeader("sec-fetch-dest", "empty")
                .build();
        tokenCount = 0;
        this.redisTemplate = redisTemplate;
        tokens = deepSeekProperty.getTokens() == null ? new ArrayList<>() : deepSeekProperty.getTokens();

        // Bearer 开头
        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (!token.startsWith("Bearer ")) tokens.add(i,"Bearer " + token);
        }
    }

    @Override
    public Map<String, Object> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        DeepSeekRequestBody requestBody = new DeepSeekRequestBody();
        // 会话id在doOnBefore设置,因为多次请求的话会话id是不一样的
        requestBody.setPrompt(JSONUtil.toJsonStr(dialogInputDTO.getMessages()));
        return BeanUtil.beanToMap(requestBody, true, false);// 大坑,不能忽略null值
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
        // 不是以data开头的,说明不是流式数据
        if (!data.startsWith("data:")) {
            JSONObject responseBody = JSONUtil.parseObj(data);
            // 说明出错了
            if (responseBody.get("code", Integer.class) != 0) {
                log.error("请求返回错误,错误信息:{},响应体为:{}", responseBody.get("msg"), responseBody.get("data"));
                return new DialogVO(List.of(new ReplyVO(index, true)));
            } else {
                log.warn("请求返回非正常数据,信息为:{},响应体为:{}", responseBody.get("msg"), responseBody.get("data"));
                Message message = new Message("assistant", responseBody.get("data", String.class));
                return new DialogVO(List.of(new CommonReplyVO(message, index, false)));
            }
        }
        String responseBody = data.substring(5);
        if (responseBody.contains("[DONE]")) return new DialogVO(List.of(new ReplyVO(index, true)));

        JSONObject jsonObject = JSONUtil.parseObj(responseBody);
        List<?> choices = jsonObject.get("choices", List.class);
        if (choices.isEmpty()) return new DialogVO(null);
        JSONObject result = JSONUtil.parseObj(choices.getFirst());

        // 存在结束标记的情况
        String finishReason = result.get("finish_reason", String.class);
        if(finishReason != null){
            return switch (finishReason){
                case "stop" ->{
                    log.info("对话正常结束");
                    yield new DialogVO(List.of(new ReplyVO(index, false)));
                }
                case "content_filter" -> {
                    log.info("对话存在违禁词,被截断了");
                    yield new DialogVO(List.of(new ReplyVO(index, true)));
                }
                default -> {
                    log.warn("对话非正常结束,原因:{}", finishReason);
                    yield new DialogVO(List.of(new ReplyVO(index, true)));
                }
            };

        }

        // 正常返回
        String content = result.get("delta", JSONObject.class).get("content", String.class);
        Message message = new Message("assistant", content);
        return new DialogVO(List.of(new CommonReplyVO(message, index, false)));
    }

    @Override
    public DialogVO notStreamResponseToDialogVO(Integer index, String data) {
        // 因为这个网站只能流式返回数据,因此处理方式和流式数据处理方式一样
        return streamResponseToDialogVO(index, data);
    }

    @Override
    public String getUrl(DialogInputDTO dialogInputDTO) {
        return "https://chat.deepseek.com/api/v0/chat/completion";
    }

    @Override
    public WebClient getWebClient() {
        return webClient;
    }

    @Override
    public boolean isStream(DialogInputDTO dialogInputDTO) {
        // deepseek网站只支持流式返回
        return true;
    }

    /**
     * 获取一个会话id
     *
     * @return 会话id字符串
     */
    private Flux<String> createSessionId(int index) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("character_id", null);
        return webClient.post()
                .uri("/chat_session/create")
                .bodyValue(requestBody)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, tokens.get((tokenCount + index) % tokens.size()))
                .retrieve()
                .bodyToFlux(JSONObject.class)
                .handle((response, sink) -> {
                    log.info(response.toString());
                    if(response.get("code", Integer.class) == 40003) {
                        sink.error(new TokenExpireException(tokens.get((tokenCount + index) % tokens.size())));
                        return;
                    }
                    sink.next(response.get("data", JSONObject.class)
                            .get("biz_data", JSONObject.class)
                            .get("id", String.class));
                });
    }

    @Override
    public Flux<HttpHeaders> getHttpHeaders(Map<String,Object> requestBody, Integer index) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, tokens.get((tokenCount + index) % tokens.size()));
        // 创建 challenge
        return createChallenge(index).flatMap(challenge -> {
                    // 计算挑战
                    String header = DeepSeekHashV1.computePowAnswer(challenge);
                    if (header == null || header.isEmpty()) return Mono.error(new RuntimeException("计算Pow失败"));
                    log.info("计算出请求头为:{}", header);

                    // 设置 header
                    headers.set(FinalNumber.DEEP_SEEK_CHALLENGE_HEADER, header);
                    return Mono.just(headers);
                })
                .flux();
    }

    /**
     * 向deepseek申请一个Pow挑战参数
     *
     * @return PowChallenge挑战参数实体类
     */
    private Mono<PowChallenge> createChallenge(Integer index) {
        return webClient.post()
                .uri("/chat/create_pow_challenge")
                .bodyValue(Map.of("target_path", FinalNumber.DEEPSEEK_TARGET_PATH))
                .header(HttpHeaders.AUTHORIZATION, tokens.get((tokenCount + index) % tokens.size()))
                .retrieve()
                .bodyToMono(JSONObject.class)
                .map(response -> response.get("data", JSONObject.class)
                        .get("biz_data", JSONObject.class)
                        .get("challenge", PowChallenge.class));
    }

    /**
     * 回复完后删除会话
     */
    @Override
    public void doOnComplete(Integer index) {
        if (index == null) return;
        String key = "DeepSeek:" + tokens.get((tokenCount + index) % tokens.size());
        redisTemplate.opsForHash()
                .get(key, FinalNumber.DEEPSEEK_SESSION_ID + index)
                .flatMap(sessionId ->
                        webClient.post()
                                .uri("/chat_session/delete")
                                .header(HttpHeaders.AUTHORIZATION, tokens.get(tokenCount))
                                .bodyValue(Map.of("chat_session_id", sessionId))
                                .retrieve()
                                .bodyToMono(JSONObject.class)
                                .doOnNext(response -> {
                                    if (response.get("code", Integer.class) != 0)
                                        log.warn("删除对话失败,原因:{},响应体为:{}", response.get("msg"), response.get("data"));
                                    else log.info("删除对话成功");
                                })
                                // 删除redis中的会话id
                                .then(redisTemplate.opsForHash().remove(key, FinalNumber.DEEPSEEK_SESSION_ID + index))
                )
                .subscribe(count -> log.info("成功删除 {} 个会话记录", count), err -> log.error("删除操作失败", err));
    }

    @Override
    public Flux<Map<String, Object>> doOnBefore(Map<String, Object> requestBody, Integer index) {
        // 创建会话id
        return createSessionId(index)
                .flatMap(sessionId -> {
                    log.info("成功创建会话id:{}", sessionId);
                    String key = "DeepSeek:" + tokens.get((tokenCount + index) % tokens.size());
                    requestBody.put("chat_session_id", sessionId);
                    return redisTemplate.opsForHash()
                            .put(key, FinalNumber.DEEPSEEK_SESSION_ID + index, sessionId)
                            .doOnNext(success -> {
                                // 存入redis是为了后续删除对话
                                if (success) log.info("成功将会话id存入redis");
                                else log.info("将会话id存入redis失败了");
                            })
                            .thenReturn(requestBody);
                });
    }

    @Override
    public void sendOnComplete() {
        tokenCount = ++tokenCount % tokens.size();
    }
}
