package com.ling.sillytavernproxy.exception;

import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.Charset;

@Getter
public class ResourceExhaustedException extends WebClientResponseException {
    private final String api;

    public ResourceExhaustedException(String api, WebClientResponseException e) {
        super(
                "API:" + api + " 额度已消耗完",
                e.getStatusCode(),
                e.getStatusText(),
                e.getHeaders(),
                e.getResponseBodyAsString().getBytes(),
                Charset.defaultCharset(),
                e.getRequest()
        );
        this.api = api;
    }
}
