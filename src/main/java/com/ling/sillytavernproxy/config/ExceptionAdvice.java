package com.ling.sillytavernproxy.config;

import com.ling.sillytavernproxy.exception.TokenExpireException;
import com.ling.sillytavernproxy.exception.UnknownModelException;
import com.ling.sillytavernproxy.vo.ErrorResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@Slf4j
public class ExceptionAdvice {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseVO> exception(Exception e) {
      log.error("未知错误,错误信息{}",e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .contentType(MediaType.APPLICATION_NDJSON)
              .body(new ErrorResponseVO(e.getMessage()));
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponseVO> timeoutException(TimeoutException e) {
        log.error("请求超时,断开连接,错误信息{}",e.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .contentType(MediaType.APPLICATION_NDJSON)
                .body(new ErrorResponseVO("请求超时,AI一分钟无回应,已断开连接,是不是忘记开梯子啦?",HttpStatus.GATEWAY_TIMEOUT.value()));
    }

    @ExceptionHandler(TokenExpireException.class)
    public ResponseEntity<ErrorResponseVO> tokenExpireException(TokenExpireException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_NDJSON)
                .body(new ErrorResponseVO(e.getMessage()));
    }

    @ExceptionHandler(UnknownModelException.class)
    public ResponseEntity<ErrorResponseVO> unknownModelException(UnknownModelException e) {
        log.error("未知的模型,异常信息为:{}",e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_NDJSON)
                .body(new ErrorResponseVO(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponseVO> webClientResponseException(WebClientResponseException e) {
        log.error("出错了,状态码为:{},返回的错误响应体为:{}", e.getStatusCode(), e.getResponseBodyAsString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_NDJSON)
                .body(new ErrorResponseVO(e.getResponseBodyAsString(), e.getStatusCode().value()));
    }
}
