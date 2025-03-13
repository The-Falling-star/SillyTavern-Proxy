package com.ling.sillytavernproxy.service;

import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public interface DialogService {

    WebClient WEB_CLIENT = WebClient.builder().build();

    Logger log = LoggerFactory.getLogger(DialogService.class);

    default Flux<DialogVO> sendDialog(DialogInputDTO dialogInputDTO) {
        doOnBefore();
        WebClient webClient = getWebClient();
        Map<String, ?> requestBody = inputToRequestBody(dialogInputDTO);
        if (dialogInputDTO.getReplyNum() == null || dialogInputDTO.getReplyNum() < 1) dialogInputDTO.setReplyNum(1);
        log.info("开始请求");

        Flux<DialogVO> flux = Flux.range(0, dialogInputDTO.getReplyNum())
                .flatMap(index -> {
                    WebClient.RequestBodyUriSpec request = webClient.post();

                    // 设置请求头
                    HttpHeaders headers = getHttpHeaders();
                    if (headers != null){
                        headers.forEach((headerName, headerValue) -> request.header(headerName, headerValue.toArray(new String[0])));
                    }

                    // 设置一些请求信息并请求
                    Flux<String> response = request.uri(getUrl())
                            .bodyValue(requestBody)
                            .accept(isStream(dialogInputDTO) ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToFlux(DataBuffer.class)
                            .timeout(Duration.ofMinutes(dialogInputDTO.isStream() ? 1 : 5))
                            .map(buffer -> {
                                try {
                                    return buffer.toString(StandardCharsets.UTF_8);
                                } finally {
                                    DataBufferUtils.release(buffer); // 必须显式释放
                                }
                            })
                            .doOnComplete(this::doOnComplete);

                    return dialogInputDTO.isStream() ? response.map(data -> this.streamResponseToDialogVO(index, data)) :
                            response.collect(Collectors.joining())
                                    .map(data -> this.notStreamResponseToDialogVO(index, data))
                                    .flux();
                });

        // 如果非流式回复,则将所有回复都收集起来
        if (!dialogInputDTO.isStream()) {
            /*
            collectList()返回的类型为Mono<List<DialogVO>>
            dialogVOS的类型为List<DialogVO>,这个数据包含了所有回复的DialogVO
            */
            flux = flux.collectList().map(dialogVOS -> {
                        // 将List内的每个DialogVO的对话内容全部提取出来,放到ret的choice数组里
                        DialogVO ret = new DialogVO(new ArrayList<>(dialogInputDTO.getReplyNum()));
                        dialogVOS.forEach(dialogVO -> {
                            CommonReplyVO commonReplyVO = (CommonReplyVO) dialogVO.getChoices().getFirst();
                            ret.getChoices().add(commonReplyVO);// TODO 这里后续要按照index设置对话
                        });
                        return ret;
                    })
                    .flux();
        }
        return flux;
    }

    /**
     * 将SillyTavern传进来的对话数据转换为第三方ai可解析的json数据
     *
     * @param dialogInputDTO SillyTavern传进来的对话数据
     * @return 一个Map的请求体
     */
    Map<String, ?> inputToRequestBody(DialogInputDTO dialogInputDTO);

    /**
     * 将第三方回复的流式数据转换为SillyTavern可解析的DialogVO对象
     *
     * @param index SillyTavern备选回复的序号
     * @param data  第三方回复的数据,可能是json,也可能是纯文字
     * @return SillyTavern可解析的DialogVO对象
     */
    DialogVO streamResponseToDialogVO(Integer index, String data);

    /**
     * 将第三方回复的非流式数据转换为SillyTavern可解析的DialogVO对象
     *
     * @param index SillyTavern备选回复的序号
     * @param data  第三方回复的数据,可能是json,也可能是纯文字
     */
    DialogVO notStreamResponseToDialogVO(Integer index, String data);

    /**
     * 获取第三方ai的请求url
     *
     * @return 第三方ai的请求url
     */
    String getUrl();

    /**
     * 请求结束后要做的事情,如请求删除对话框
     */
    default void doOnComplete(Object... params) {
    }

    /**
     * 自定义webClient
     *
     * @return webClient实例
     */
    default WebClient getWebClient() {
        return WEB_CLIENT;
    }

    /**
     * 确定对第三方api是否为流式回复
     *
     * @param dialogInputDTO 输入对象
     * @return 如果需要第三方api流式返回则返回true
     */
    default boolean isStream(DialogInputDTO dialogInputDTO) {
        return dialogInputDTO.isStream();
    }

    /**
     * 在请求之前需要做的事情
     */
    default void doOnBefore() {
    }

    /**
     * 获取请求头
     *
     * @return HttpHeaders对象
     */
    default HttpHeaders getHttpHeaders() {
        return null;
    }
}
