package com.ling.sillytavernproxy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperty {
    private List<String> apis;
}
