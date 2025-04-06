package com.ling.sillytavernproxy.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ling.sillytavernproxy.config.FinalNumber;
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
    GEMINI_2_FLASH(FinalNumber.MODEL_GEMINI_2_FLASH,FinalNumber.MODEL_GEMINI_2_FLASH),
    GEMINI_2_FLASH_STABLE(FinalNumber.MODEL_GEMINI_2_FLASH_STABLE,"gemini-2.0-flash-001"),
    GEMINI_2_FLASH_EXP(FinalNumber.MODEL_GEMINI_2_FLASH_EXP,"gemini-2.0-flash-exp"),
    GEMINI_2_FLASH_THINKING_EXP_0121(FinalNumber.MODEL_GEMINI_2_FLASH_THINKING_EXP_0121, "gemini-2.0-flash-thinking-exp-01-21"),
    // GEMINI_2_FLASH_THINKING_EXP_1219(MODEL_GEMINI_2FLASH_THINKING_EXP_1219,"gemini-2.0-flash-thinking-exp-1219"),
    // GEMINI_2_PRO_EXP_0205 (FinalNumber.MODEL_GEMINI_2_PRO_EXP_0205, "gemini-2.0-pro-exp-02-05"),
    // GEMINI_2_PRO_EXP(FinalNumber.MODEL_GEMINI_2_PRO_EXP, "gemini-2.0-pro-exp"),
    GEMINI_2__5_PRO_EXP(MODEL_GEMINI_2__5_PRO_EXP,"gemini-2.5-pro-exp-03-25"),
    GEMINI___5_PRO_PREVIEW(MODEL_GEMINI_2__5_PRO_PREVIEW,"gemini-2.5-pro-preview-03-25"),
    UNKNOWN("unknown","unknown");

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
            if(model == UNKNOWN) continue;
            if(model.getId().equals(id)) return model;
        }
        return UNKNOWN;
    }
}
