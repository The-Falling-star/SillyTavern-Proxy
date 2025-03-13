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
import com.ling.sillytavernproxy.util.DeepSeekHashV1;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService implements DialogService {

    private final WebClient webClient;

    public DeepSeekService() {
        webClient = WebClient.builder().baseUrl("https://chat.deepseek.com/api/v0").build();
    }

    @Override
    public Map<String, ?> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        String sessionId = createSessionId();
        DeepSeekRequestBody requestBody = new DeepSeekRequestBody();
        requestBody.setPrompt(JSONUtil.toJsonStr(dialogInputDTO.getMessages()));
        requestBody.setChatSessionId(sessionId);
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
        return webClient.post()
                .uri("/chat_session/create")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(CommonResponse.class)
                .map(response -> JSONUtil.parseObj(response.getData())
                        .get("biz_data", JSONObject.class)
                        .get("id", String.class))
                .block();
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // 计算挑战
        String challenge = DeepSeekHashV1.computePowAnswer(createChallenge());
        headers.set("X-Ds-Pow-Response", challenge);
        return headers;
    }

    /**
     * 像deepseek申请一个Pow挑战参数
     *
     * @return PowChallenge挑战参数实体类
     */
    private PowChallenge createChallenge() {
        // TODO 以后要做一个缓存
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
    public void doOnComplete() {
        // TODO 用缓存实现获取要删除的会话id
        webClient.post()
                .uri("chat_session/delete")
                .bodyValue(Map.of("chat_session_id", ""))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), res -> {
                    log.info("删除对话失败");
                    return null;
                });
    }
}
