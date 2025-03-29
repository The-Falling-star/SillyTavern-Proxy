package com.ling.sillytavernproxy.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class UnknownModelException extends RuntimeException{
    public UnknownModelException(){
        super("请求的模型暂不支持");
    }

}
