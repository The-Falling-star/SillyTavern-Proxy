package com.ling.sillytavernproxy.config;

import java.util.ArrayList;
import java.util.List;

public class FinalNumber {
    public static final String XIAO_BAI_URL = "https://api-bj.wenxiaobai.com/api/v1.0/core/conversations";
    public static int XIAO_BAI_USERID;
    public static final List<String> XIAO_BAI_TOKENS = new ArrayList<>();
    private static int XIAO_BAI_TOKEN_INDEX = 0;
    public static String getXiaoBaiToken() {
        return XIAO_BAI_TOKENS.get((XIAO_BAI_TOKEN_INDEX++) % XIAO_BAI_TOKENS.size());
    }

    public static final String ZAI_WEN_URL = "https://aliyun.zaiwen.top/admin";
    public static final String DEEPSEEK_TARGET_PATH = "/api/v0/chat/completion";
    public static final String DEEP_SEEK_CHALLENGE_HEADER = "X-Ds-Pow-Response";
    public static final String DEEP_SEEK_BASE_URL = "https://chat.deepseek.com/api/v0";

    // 模型常量
    public static final String MODEL_ZAI_WEN_DEEPSEEK = "zaiWen-deepseek-reasoner";
    public static final String MODEL_DEEPSEEK = "deepSeekR1";
    public static final String MODEL_WEN_XIAO_BAI_DEEPSEEK = "wenXiaoBai-deepseek";

    public static final String MODEL_GEMINI_2_FLASH = "gemini-2.0-flash";
    public static final String MODEL_GEMINI_2_FLASH_STABLE = "gemini-2.0-flash-001";
    public static final String MODEL_GEMINI_2_FLASH_EXP = "gemini-2.0-flash-exp";
    public static final String MODEL_GEMINI_2_FLASH_THINKING_EXP = "gemini-2.0-flash-thinking-exp-01-21";
    public static final String MODEL_GEMINI_2_PRO_EXP_0205 = "gemini-2.0-pro-exp-02-05";
    public static final String MODEL_GEMINI_2_PRO_EXP = "gemini-2.0-pro-exp";

    public static final String DEEPSEEK_SESSION_ID = "session_id";
}

