package com.ling.sillytavernproxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ModelConfig {

    @Bean("models")
    public Map<String,String> getModels(){
        return Map.of("zaiWen-deepseek-reasoner","deepseek-reasoner",
                "wenXiaoBai-deepseek","200006",
                "deepSeekR1","deepSeekR1");
    }
}
