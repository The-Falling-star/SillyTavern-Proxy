package com.ling.sillytavernproxy.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommonResponse {
    private int code;
    private String msg;
    private Object data;

    public CommonResponse(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public CommonResponse(Object data){
        this.data = data;
        code = 200;
        msg = "success";
    }
}
