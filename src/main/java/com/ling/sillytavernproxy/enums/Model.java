package com.ling.sillytavernproxy.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ling.sillytavernproxy.exception.UnknownModelException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.ling.sillytavernproxy.config.FinalNumber.*;

@Getter
@RequiredArgsConstructor
public enum Model {
    ZAI_WEN_DEEPSEEK(MODEL_ZAI_WEN_DEEPSEEK,"deepseek-reasoner"),
    DEEPSEEK(MODEL_DEEPSEEK,"deepSeekR1"),
    WEN_XIAO_BAI_DEEPSEEK(MODEL_WEN_XIAO_BAI_DEEPSEEK,"200006"),

    // 谷歌gemini的模型
    GEMINI_2_FLASH(MODEL_GEMINI_2_FLASH,MODEL_GEMINI_2_FLASH),
    GEMINI_2_FLASH_STABLE(MODEL_GEMINI_2_FLASH_STABLE,"gemini-2.0-flash-001"),
    GEMINI_2_FLASH_EXP( MODEL_GEMINI_2_FLASH_EXP,"gemini-2.0-flash-exp"),
    GEMINI_2_FLASH_THINKING_EXP (MODEL_GEMINI_2_FLASH_THINKING_EXP, "gemini-2.0-flash-thinking-exp-01-21"),
    GEMINI_2_PRO_EXP_0205 (MODEL_GEMINI_2_PRO_EXP_0205, "gemini-2.0-pro-exp-02-05"),
    GEMINI_2_PRO_EXP(MODEL_GEMINI_2_PRO_EXP, "gemini-2.0-pro-exp"),
    UNKNOWN;

    @JsonValue
    private final String id; // 对SillyTavern显示的值

    private final String out; // 对外显示的值

    Model(){
        this.id = null;
        this.out = null;
    }

    @JsonCreator
    public static Model fromString(String id) {
        if(id == null || id.isEmpty()) return null;
        for(Model model : values()) {
            if(model.getId().equals(id)) return model;
        }
        throw new UnknownModelException(id);
    }
}
