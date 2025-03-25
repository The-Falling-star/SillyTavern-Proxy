package com.ling.sillytavernproxy.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class UnknownModelException extends RuntimeException{
    String model;
    String message;
    public UnknownModelException(String model){
        this.model = model;
        this.message = "模型:" + model + "暂不支持";
    }

}
