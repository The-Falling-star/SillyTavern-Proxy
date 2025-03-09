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
}
