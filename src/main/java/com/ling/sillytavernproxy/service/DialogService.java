package com.ling.sillytavernproxy.service;

import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.exception.TokenExpireException;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface DialogService {

    WebClient WEB_CLIENT = WebClient.builder().build();

    Logger log = LoggerFactory.getLogger(DialogService.class);

    /**
     * 核心方法,发送对话
     *
     * @param dialogInputDTO SillyTavern输入的请求体
     * @return 返回流式的DialogVO数据
     */
    default Flux<DialogVO> sendDialog(DialogInputDTO dialogInputDTO) {
        WebClient webClient = this.getWebClient();
        Map<String, ?> body = inputToRequestBody(dialogInputDTO);
        int replyNum = dialogInputDTO.getReplyNum() == null || dialogInputDTO.getReplyNum() == 0 ?
                1 : dialogInputDTO.getReplyNum();

        this.sendOnBefore(dialogInputDTO);

        Flux<DialogVO> flux = Flux.range(0, replyNum)
                .flatMap(index -> this.doOnBefore(new HashMap<>(body), index)// 对每次请求的请求体做一个自定义前置操作
                        .flatMap(requestBody -> this.getHttpHeaders(requestBody, index)
                                .flatMap(headers -> {
                                    WebClient.RequestBodyUriSpec request = webClient.post();

                                    if (headers != null) {
                                        headers.forEach((headerName, headerValue) -> request.header(headerName, headerValue.toArray(new String[0])));
                                    }

                                    log.info("开始第{}次请求", index);
                                    // 设置一些请求信息并请求
                                    Flux<String> response = request.uri(getUrl(dialogInputDTO), uriBuilder -> this.getUriParam(uriBuilder, dialogInputDTO))
                                            .bodyValue(requestBody)
                                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                            .accept(this.isStream(dialogInputDTO) ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON)
                                            .retrieve()
                                            .bodyToFlux(DataBuffer.class)
                                            .timeout(Duration.ofMinutes(dialogInputDTO.isStream() ? 1 : 3))
                                            .map(buffer -> {
                                                log.info("接收到第{}个对话的回复:{}", index, buffer.toString(StandardCharsets.UTF_8));
                                                try {
                                                    return buffer.toString(StandardCharsets.UTF_8);
                                                } finally {
                                                    DataBufferUtils.release(buffer); // 必须显式释放
                                                }
                                            })
                                            .onErrorResume(TimeoutException.class, throwable -> {
                                                log.error("请求超时了,是不是没开梯子?\n异常信息:{}", throwable.getMessage());
                                                return Flux.error(throwable);
                                            })
                                            .onErrorResume(TokenExpireException.class, throwable -> {
                                                if (
                                                        headers == null ||
                                                        headers.get(this.getAuthorization()) == null ||
                                                        headers.get(this.getAuthorization()).isEmpty())
                                                {
                                                    log.error("没有token,是不是忘记配置啦?");
                                                }
                                                else {
                                                    log.error("token:{}过期了,重新抓取吧.\n异常信息:{}",
                                                            headers.get(this.getAuthorization()).getFirst(),
                                                            throwable.getMessage());
                                                    throwable.setToken(headers.get(this.getAuthorization()).getFirst());
                                                }
                                                return Flux.error(throwable);
                                            })
                                            .onErrorResume(WebClientResponseException.class,exception ->{
                                                log.error("请求出错了,状态码为:{},返回的错误响应体为:{}",
                                                        exception.getStatusCode(),
                                                        exception.getResponseBodyAsString());
                                                return Flux.error(exception);
                                            })
                                            .onErrorResume(throwable -> {
                                                log.error("出错了,错误信息是:{}", throwable.getMessage());
                                                return Flux.error(throwable);
                                            })
                                            .onErrorComplete();


                                    // 部分网站的流式数据和非流式数据返回格式不同,因此对于不同的处理方法
                                    return this.isStream(dialogInputDTO) ?
                                            response.map(data -> streamResponseToDialogVO(index, data))
                                                    .onErrorContinue((throwable, data) ->
                                                            log.error("转换元素出错了,导致出错的元素是:{} 异常信息为:{}",
                                                            data.toString(),
                                                            throwable.getMessage()))
                                                    .doOnComplete(() -> doOnComplete(index)) :
                                            response.map(data -> notStreamResponseToDialogVO(index, data))
                                                    .onErrorContinue((throwable, data) ->
                                                            log.error("非流式转换元素出错了,导致出错的元素是:{} 异常信息为:{}",
                                                            data.toString(),
                                                            throwable.getMessage()));
                                })));

        // 如果SillyTavern要求非流式回复,则将所有回复都收集起来统一返回
        if (!dialogInputDTO.isStream()) {
            /*
            collectList()返回的类型为Mono<List<DialogVO>>
            dialogVOS的类型为List<DialogVO>,这个数据包含了所有回复的DialogVO
            */
            flux = flux.collectList().map(dialogVOS -> {
                        // 将List内的每个DialogVO的对话内容全部提取出来,放到ret的choice数组里
                        DialogVO result = new DialogVO(new ArrayList<>(Collections.nCopies(replyNum, null)));
                        Map<Integer, StringBuilder> map = new HashMap<>(replyNum);

                        // 根据map来区分对话属于哪一个index
                        dialogVOS.forEach(dialogVO -> {
                            Integer key = dialogVO.getChoices().getFirst().getIndex();
                            StringBuilder content = map.computeIfAbsent(key, k -> new StringBuilder());

                            // 因为按先后顺序插入的,所以直接拼接就好
                            if (dialogVO.getChoices().getFirst() instanceof CommonReplyVO commonReplyVO)
                                content.append(commonReplyVO.getMessage().getContent());
                        });

                        // 将对话放入结果的数组里
                        map.forEach((key, value) -> {
                            Message message = new Message("assistant", value.toString());
                            CommonReplyVO commonReplyVO = new CommonReplyVO(message, key, false);
                            result.getChoices().set(key, commonReplyVO);
                        });
                        return result;
                    })
                    .flux();
        }
        this.sendOnComplete();
        return flux;
    }

    /**
     * 将SillyTavern传进来的对话数据转换为第三方ai可解析的json数据
     *
     * @param dialogInputDTO SillyTavern传进来的对话数据
     * @return 一个Map的请求体
     */
    Map<String, Object> inputToRequestBody(DialogInputDTO dialogInputDTO);

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
    String getUrl(DialogInputDTO dialogInputDTO);

    /**
     * 在每次请求结束后要做的事情,如请求删除对话框
     */
    default void doOnComplete(Integer index) {
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
     * 确定对第三方api是否为流式回复,
     * dialogInputDTO中的isStream是给SillyTavern看的,
     * 这个isStream是给第三方api看的,指示第三方api是否需要流式回复
     * 比如有一些镜像网站只能流式回复,那么这里就只能返回true
     *
     * @param dialogInputDTO 输入对象
     * @return 如果需要第三方api流式返回则返回true
     */
    default boolean isStream(DialogInputDTO dialogInputDTO) {
        return dialogInputDTO.isStream();
    }

    /**
     * 在每次发送对话前需要做的事情
     */
    default Flux<Map<String, Object>> doOnBefore(Map<String, Object> requestBody, Integer index) {
        return Flux.just(requestBody);
    }

    /**
     * 获取请求头
     *
     * @return HttpHeaders对象
     */
    default Flux<HttpHeaders> getHttpHeaders(Map<String, Object> requestBody, Integer index) {
        return Flux.just(new HttpHeaders());
    }

    /**
     * 在总请求之前需要做的事情
     */
    default void sendOnBefore(DialogInputDTO dialogInputDTO) {
    }

    /**
     * 在总请求之后需要做的事情
     */
    default void sendOnComplete() {
    }

    default URI getUriParam(UriBuilder uriBuilder, DialogInputDTO dialogInputDTO) {
        return uriBuilder.build();
    }

    /**
     * 获取Token的请求头的键名称
     */
    default String getAuthorization() {
        return "Authorization";
    }
}
