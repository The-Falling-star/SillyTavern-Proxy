package com.ling.sillytavernproxy.config;

import com.ling.sillytavernproxy.exception.UnknownModelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@Slf4j
public class ExceptionAdvice {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exception(Exception e) {
      log.error("未知错误,错误信息{}",e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<String> timeoutException(TimeoutException e) {
        log.error("请求超时,断开连接,错误信息{}",e.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("AI两分钟无回应,已断开连接");
    }

    @ExceptionHandler(UnknownModelException.class)
    public ResponseEntity<String> unknownModelException(UnknownModelException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
