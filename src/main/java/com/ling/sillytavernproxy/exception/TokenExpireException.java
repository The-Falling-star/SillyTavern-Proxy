package com.ling.sillytavernproxy.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenExpireException extends RuntimeException {
    private String token;
    public TokenExpireException(String token) {
        super(token == null || token.isEmpty() ? "没有携带token喔,是不是没有配置啊?" : "token:" + token + "已过期");
        this.token = token;
    }

    public TokenExpireException() {
        super("token已过期或根本没有携带token");
    }
}
