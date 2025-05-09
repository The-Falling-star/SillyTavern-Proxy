package com.ling.sillytavernproxy.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.Gemini.Content;
import com.ling.sillytavernproxy.entity.Gemini.Text;
import com.ling.sillytavernproxy.entity.Gemini.request.GeminiRequestBody;
import com.ling.sillytavernproxy.entity.Gemini.request.GenerationConfig;
import com.ling.sillytavernproxy.entity.Gemini.request.SystemInstruction;
import com.ling.sillytavernproxy.entity.Gemini.response.Candidate;
import com.ling.sillytavernproxy.entity.Gemini.response.GenerateContentResponse;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.enums.gemini.FinishReason;
import com.ling.sillytavernproxy.exception.ResourceExhaustedException;
import com.ling.sillytavernproxy.properties.GeminiProperty;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import com.ling.sillytavernproxy.vo.reply.ReplyVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service("geminiService")
public class GeminiService implements DialogService {
    private static int curApi;
    private final WebClient webClient;
    private final List<String> apis;

    public GeminiService(GeminiProperty property) {
        webClient = WebClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();
        apis = property.getApis();
        curApi = 0;
    }


    @Override
    public Flux<DialogVO> sendDialog(DialogInputDTO dialogInputDTO) {
        log.info("开始请求");
        Integer n = dialogInputDTO.getReplyNum();

        // 流式生成多个备选回复的话则需要多次调用接口了
        if (dialogInputDTO.isStream() && n != null && n > 1) dialogInputDTO.setReplyNum(1);
        else n = 1;

        Map<String, ?> body = inputToRequestBody(dialogInputDTO);
        return Flux.range(0, n)
                .flatMap(index -> {
                    String api = getApiKey();
                    return webClient.post()
                            .uri(getUrl(dialogInputDTO), uriBuilder -> getUriParam(uriBuilder, dialogInputDTO, api))
                            .bodyValue(body)
                            .accept(isStream(dialogInputDTO) ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToFlux(GenerateContentResponse.class)
                            .timeout(Duration.ofMinutes(dialogInputDTO.isStream() ? 1 : 3))
                            .onErrorResume(TimeoutException.class, throwable -> {
                                log.error("请求超时了,是不是没开梯子?\n异常信息:{}", throwable.getMessage());
                                return Flux.error(throwable);
                            })
                            .onErrorResume(WebClientResponseException.class, e -> {
                                log.error("出错了,状态码为:{},返回的错误响应体为:{}", e.getStatusCode(), e.getResponseBodyAsString());
                                return e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS ?
                                        Flux.error(new ResourceExhaustedException(api, e)) :
                                        Flux.error(e);
                            })
                            .map(response -> isStream(dialogInputDTO) ? streamBodyToDialogVO(response, index) : bodyToDialogVO(response))
                            .onErrorContinue((throwable, data) -> log.error("转换元素出错了,导致出错的元素是:{} 异常信息为:{}",
                                    data.toString(),
                                    throwable.getMessage()));
                });
    }

    @Override
    public Map<String, Object> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        GeminiRequestBody geminiRequestBody = new GeminiRequestBody();
        List<Text> parts = new ArrayList<>();
        List<Content> contents = new ArrayList<>();

        for (Message message : dialogInputDTO.getMessages()) {
            if (message.getRole().equals("system")) parts.add(new Text(message.getContent()));
            else if (message.getRole().equals("user"))
                contents.add(new Content(message.getRole(), message.getContent()));
            else contents.add(new Content("model", message.getContent()));
        }
        geminiRequestBody.setContents(contents);
        geminiRequestBody.setSystem_instruction(new SystemInstruction(parts));

        if (isStream(dialogInputDTO)) dialogInputDTO.setReplyNum(1);
        GenerationConfig generationConfig = new GenerationConfig(dialogInputDTO);

        geminiRequestBody.setGenerationConfig(generationConfig);
        return BeanUtil.beanToMap(geminiRequestBody);
    }

    @Override
    public DialogVO streamResponseToDialogVO(Integer index, String data) {
        GenerateContentResponse response = JSONUtil.toBean(data, GenerateContentResponse.class);
        return streamBodyToDialogVO(response, index);
    }

    @Override
    public DialogVO notStreamResponseToDialogVO(Integer index, String data) {
        GenerateContentResponse response = JSONUtil.toBean(data, GenerateContentResponse.class);
        return bodyToDialogVO(response);
    }

    @Override
    public String getUrl(DialogInputDTO dialogInputDTO) {
        String url = isStream(dialogInputDTO) ?
                "/v1beta/models/{model}:streamGenerateContent" :
                "/v1beta/models/{model}:generateContent";
        log.info("发送的url为:{}", url);
        return url;
    }

    @Override
    public WebClient getWebClient() {
        return webClient;
    }

    @Override
    public URI getUriParam(UriBuilder uriBuilder, DialogInputDTO dialogInputDTO) {
        return uriBuilder.queryParam("key", getApiKey()).build(dialogInputDTO.getModel().getOut());
    }

    public URI getUriParam(UriBuilder uriBuilder, DialogInputDTO dialogInputDTO, String key) {
        return uriBuilder.queryParam("key", key).build(dialogInputDTO.getModel().getOut());
    }

    private String getApiKey() {
        String api = apis.get(curApi);
        curApi = (++curApi % apis.size());
        return api;
    }

    /**
     * 非流式回复片段转换为DilogVO
     *
     * @param response gemini回复的信息
     */
    private DialogVO bodyToDialogVO(GenerateContentResponse response) {
        log.info("接收到来自gemini的回复:{}", JSONUtil.toJsonStr(response));
        int index = 0;
        ArrayList<ReplyVO> replyVOS = new ArrayList<>();
        for (Candidate candidate : response.getCandidates()) {
            // 非流式回复响应包含多个对话的回复,因此多次调用辅助函数
            constructDialogVO(replyVOS, candidate, index);
            index++;
        }
        return new DialogVO(replyVOS);
    }

    /**
     * 流式回复片段转换为DilogVO
     *
     * @param response gemini回复的信息
     * @param index    第index个对话
     */
    private DialogVO streamBodyToDialogVO(GenerateContentResponse response, int index) {
        log.info("接收到来自gemini的第{}个对话的回复:{}", index, JSONUtil.toJsonStr(response));
        ArrayList<ReplyVO> replyVOS = new ArrayList<>();
        // 流式的多个回复是通过发送多次请求,因此每次返回的数据都是只有一个对话的信息,因此调用一次辅助函数就行
        if (!response.getCandidates().isEmpty())
            constructDialogVO(replyVOS, response.getCandidates().getFirst(), index);

        return new DialogVO(replyVOS);
    }

    /**
     * 构造DialogVO的辅助函数,减少重复代码
     *
     * @param index     属于第几个对话的回复
     * @param replyVOS  DialogVO的choice数组
     * @param candidate 回复信息
     */
    private void constructDialogVO(ArrayList<ReplyVO> replyVOS, Candidate candidate, int index) {
        FinishReason finishReason = candidate.getFinishReason();

        if (finishReason != null && finishReason != FinishReason.STOP) {
            log.warn("回复被截断咯,截断原因是:{}", finishReason);
            return;
        }
        String content = "";
        List<Text> parts;
        if (
                candidate.getContent() != null &&
                (parts = candidate.getContent().getParts()) != null &&
                !parts.isEmpty()
        ) content = parts.getFirst().getText() == null ? "" : parts.getFirst().getText();

        Message message = new Message("model", content);
        CommonReplyVO commonReplyVO = new CommonReplyVO(message, index, !content.isEmpty());
        replyVOS.add(commonReplyVO);
    }
}
