package com.ling.sillytavernproxy.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ling.sillytavernproxy.config.FinalNumber;
import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.CommonResponse;
import com.ling.sillytavernproxy.entity.DeepSeek.DeepSeekRequestBody;
import com.ling.sillytavernproxy.entity.DeepSeek.PowChallenge;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.properties.DeepSeekProperty;
import com.ling.sillytavernproxy.util.DeepSeekHashV1;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("deepSeekService")
public class DeepSeekService implements DialogService {

    private final WebClient webClient;

    private final List<String> tokens;

    private static int tokenCount;

    RedisTemplate<String,String> redisTemplate;

    RestTemplate restTemplate;

    public DeepSeekService(RedisTemplate<String,String> redisTemplate, DeepSeekProperty deepSeekProperty, RestTemplate restTemplate) {
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
        requestBody.setPrompt(JSONUtil.toJsonStr(dialogInputDTO.getMessages()));
        return BeanUtil.beanToMap(requestBody);
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
    private String createSessionId() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("character_id", null);
        CommonResponse response = restTemplate.postForObject(FinalNumber.DEEP_SEEK_BASE_URL + "/chat_session/create",
                        requestBody,
                        CommonResponse.class);
        return JSONUtil.parseObj(response.getData())
                .get("biz_data", JSONObject.class)
                .get("id", String.class);
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String key = "DeepSeek:" + tokens.get(tokenCount);
        String expireTime = (String) redisTemplate.opsForHash().get(key, FinalNumber.DEEPSEEK_CHALLENGE_EXPIRE);

        // 如果没找到过期时间或者Pow已经过期
        if(expireTime == null || Instant.ofEpochMilli(Long.parseLong(expireTime)).isBefore(Instant.now())) {
            PowChallenge challenge = createChallenge();

            // 计算挑战
            String header = DeepSeekHashV1.computePowAnswer(challenge);
            if(header == null || header.isEmpty()){
                throw new RuntimeException("计算不出deepseek请求头");
            }

            // 存入redis
            redisTemplate.opsForHash().put(key, FinalNumber.DEEPSEEK_CHALLENGE_EXPIRE, challenge.getExpireAt());
            redisTemplate.opsForHash().put(key, FinalNumber.DEEP_SEEK_CHALLENGE_HEADER, header);
            headers.set(FinalNumber.DEEP_SEEK_CHALLENGE_HEADER, header);
        }
        else headers.set(FinalNumber.DEEP_SEEK_CHALLENGE_HEADER, (String) redisTemplate.opsForHash().get(key,FinalNumber.DEEP_SEEK_CHALLENGE_HEADER));

        headers.set("Authorization",tokens.get(tokenCount));
        return headers;
    }

    /**
     * 像deepseek申请一个Pow挑战参数
     *
     * @return PowChallenge挑战参数实体类
     */
    private PowChallenge createChallenge() {
        return webClient.post()
                .uri("chat/create_pow_challenge")
                .bodyValue(Map.of("target_path", FinalNumber.DEEPSEEK_TARGET_PATH))
                .retrieve()
                .bodyToMono(PowChallenge.class)
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
    public void doOnBefore(Map<String, Object> requestBody, Integer index){
        // 创建会话id
        String sessionId = createSessionId();
        String key = "DeepSeek:" + tokens.get(tokenCount);
        requestBody.put("chat_session_id",sessionId);
        redisTemplate.opsForHash().put(key, FinalNumber.DEEPSEEK_SESSION_ID + index, sessionId);
    }

    @Override
    public void sendOnBefore(DialogInputDTO dialogInputDTO) {
        tokenCount = ++tokenCount % tokens.size();
    }
}
