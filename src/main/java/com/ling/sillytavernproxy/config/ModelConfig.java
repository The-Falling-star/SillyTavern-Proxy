package com.ling.sillytavernproxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ModelConfig {

    @Bean("models")
    public Map<String,String> getModels(){
        return Map.of(FinalNumber.MODEL_ZAI_WEN_DEEPSEEK,"deepseek-reasoner",
                FinalNumber.MODEL_WEN_XIAO_BAI_DEEPSEEK,"200006",
                FinalNumber.MODEL_DEEPSEEK,"deepSeekR1");
    }
}
