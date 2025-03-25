package com.ling.sillytavernproxy.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.Gemini.Content;
import com.ling.sillytavernproxy.entity.Gemini.Text;
import com.ling.sillytavernproxy.entity.Gemini.request.*;
import com.ling.sillytavernproxy.entity.Gemini.response.Candidate;
import com.ling.sillytavernproxy.entity.Gemini.response.GenerateContentResponse;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.enums.gemini.FinishReason;
import com.ling.sillytavernproxy.properties.GeminiProperty;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import com.ling.sillytavernproxy.vo.reply.ReplyVO;
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

@Service("geminiService")
public class GeminiService implements DialogService {
    private final WebClient webClient;

    private final List<String> apis;

    private static int curApi;

    public GeminiService(GeminiProperty property) {
        webClient = WebClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();
        apis = property.getApis();
        curApi = 0;
    }


    @Override
    public Flux<DialogVO> sendDialog(DialogInputDTO dialogInputDTO) {
        // TODO 需要解决流式回复不能一次生成多个备选回复的问题
        Map<String, ?> body = inputToRequestBody(dialogInputDTO);
        log.info("开始请求");
        return webClient.post()
                .uri(getUrl(dialogInputDTO),uriBuilder -> getUriParam(uriBuilder,dialogInputDTO))
                .bodyValue(body)
                .accept(isStream(dialogInputDTO) ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(GenerateContentResponse.class)
                .timeout(Duration.ofMinutes(dialogInputDTO.isStream() ? 1 : 5))
                .map(this::bodyToDialogVO)
                .doOnError(WebClientResponseException.class, e -> log.error("出错了,返回的错误响应体为:{}",e.getResponseBodyAsString()));
    }

    @Override
    public Map<String, Object> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        GeminiRequestBody geminiRequestBody = new GeminiRequestBody();
        List<Text> parts = new ArrayList<>();
        List<Content> contents = new ArrayList<>();

        for (Message message : dialogInputDTO.getMessages()) {
            if(message.getRole().equals("system")) parts.add(new Text(message.getContent()));
            else if(message.getRole().equals("user")) contents.add(new Content(message.getRole(),message.getContent()));
            else contents.add(new Content("model",message.getContent()));
        }
        geminiRequestBody.setContents(contents);
        geminiRequestBody.setSystem_instruction(new SystemInstruction(parts));

        GenerationConfig generationConfig = new GenerationConfig(dialogInputDTO);
        geminiRequestBody.setGenerationConfig(generationConfig);
        return BeanUtil.beanToMap(geminiRequestBody);
    }

    @Override
    public DialogVO streamResponseToDialogVO(Integer index, String data) {
        GenerateContentResponse response = JSONUtil.toBean(data, GenerateContentResponse.class);
        return bodyToDialogVO(response);
    }

    @Override
    public DialogVO notStreamResponseToDialogVO(Integer index, String data) {
        GenerateContentResponse response = JSONUtil.toBean(data, GenerateContentResponse.class);
        return bodyToDialogVO(response);
    }

    @Override
    public String getUrl(DialogInputDTO dialogInputDTO) {
        return isStream(dialogInputDTO) ?
                "/v1beta/models/{model}:streamGenerateContent" :
                "/v1beta/models/{model}:generateContent";
    }

    @Override
    public WebClient getWebClient() {
        return webClient;
    }

    @Override
    public URI getUriParam(UriBuilder uriBuilder, DialogInputDTO dialogInputDTO) {
        if(isStream(dialogInputDTO)) uriBuilder.queryParam("alt","sse");
        return uriBuilder.queryParam("key", getApiKey()).build(dialogInputDTO.getModel().getOut());
    }

    private String getApiKey(){
        String api = apis.get(curApi);
        curApi = (++curApi % apis.size());
        return api;
    }

    private DialogVO bodyToDialogVO(GenerateContentResponse response) {
        log.info("接收到来自gemini的回复:{}",JSONUtil.toJsonStr(response));
        int index = 0;
        ArrayList<ReplyVO> replyVOS = new ArrayList<>();
        for(Candidate candidate: response.getCandidates()){
            FinishReason finishReason = candidate.getFinishReason();

            if(finishReason != null && finishReason != FinishReason.STOP){
                log.warn("回复被截断咯,截断原因是:{}",finishReason);
                continue;
            }

            String content = candidate.getContent().getParts().getFirst().getText();
            Message message = new Message("model", content);
            CommonReplyVO commonReplyVO = new CommonReplyVO(message, index, false);
            replyVOS.add(commonReplyVO);
            index++;
        }
        return new DialogVO(replyVOS);
    }
}
