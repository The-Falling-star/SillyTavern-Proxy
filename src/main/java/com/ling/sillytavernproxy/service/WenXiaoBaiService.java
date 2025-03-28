package com.ling.sillytavernproxy.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.ling.sillytavernproxy.config.FinalNumber;
import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.entity.WenXiaoBai.WenXiaoBaiRequestBody;
import com.ling.sillytavernproxy.enums.Model;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service("wenXiaoBaiService")
@Slf4j
public class WenXiaoBaiService implements DialogService {

    WebClient webClient;

    public WenXiaoBaiService() {
        webClient = WebClient
                .builder()
                // .defaultHeader("X-Yuanshi-Authorization", FinalNumber.getXiaoBaiToken())
                .build();
    }

    @Override
    public Map<String, Object> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        WenXiaoBaiRequestBody wenXiaoBaiRequestBody = new WenXiaoBaiRequestBody();
        wenXiaoBaiRequestBody.setBotId(dialogInputDTO.getModel().getOut());
        wenXiaoBaiRequestBody.setConversationId(createConversation(Integer.parseInt(Model.WEN_XIAO_BAI_DEEPSEEK.getOut())));
        wenXiaoBaiRequestBody.setTurnIndex(0);
        wenXiaoBaiRequestBody.setQuery(JSONUtil.toJsonStr(dialogInputDTO.getMessages()));
        return BeanUtil.beanToMap(wenXiaoBaiRequestBody);
    }

    @Override
    public DialogVO streamResponseToDialogVO(Integer index, String data) {
        Message msg = new Message();
        msg.setRole("assistant");
        msg.setContent(data);
        return new DialogVO(List.of(new CommonReplyVO(msg, 0, data == null || data.isEmpty())));
    }

    @Override
    public DialogVO notStreamResponseToDialogVO(Integer index, String data) {
        return streamResponseToDialogVO(index,data);
    }

    @Override
    public String getUrl(DialogInputDTO dialogInputDTO) {
        return FinalNumber.XIAO_BAI_URL + "/chat/v1";
    }

    /**
     * 新增会话
     *
     * @param botId 模型id 包括200005开启联网搜索,200004不联网,适合写作,200006官网原版
     * @return 返回会话id
     */
    private String createConversation(int botId) {
        String url = "/users/" + FinalNumber.XIAO_BAI_USERID + "/bots/" + botId + "/conversation";
        return webClient.post()
                .uri(url)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.info("出错信息为:{}", body);
                                    return Mono.error(new RuntimeException());
                                })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(response -> {  // 使用 map 操作符处理响应
                    if (response.get("code") == null || response.get("code").equals(0)) {
                        return response.get("msg").toString();
                    } else {
                        return response.get("data").toString();
                    }
                })
                .block();
    }

    @Override
    public void doOnComplete(Integer index) {
        String url = "/users/" + FinalNumber.XIAO_BAI_USERID + "/bots";
        webClient.post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
    }

    @Override
    public WebClient getWebClient(){
        return webClient;
    }
}

