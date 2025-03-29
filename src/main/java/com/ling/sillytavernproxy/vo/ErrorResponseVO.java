package com.ling.sillytavernproxy.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ErrorResponseVO {
    private ErrorResponse error;
    public ErrorResponseVO(String message) {
        this.error = new ErrorResponse(message,500);
    }
    public ErrorResponseVO(String error, int code) {
        this.error = new ErrorResponse(error,code);
    }
}

@Data
@AllArgsConstructor
class ErrorResponse {
    private String message;
    private int code;

}
