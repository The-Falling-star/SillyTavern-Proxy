package com.ling.sillytavernproxy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperty {
    private List<String> tokens;
}
