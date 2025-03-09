package com.ling.sillytavernproxy.entity.WenXiaoBai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WenXiaoBaiRequestBody {
    private AttachmentInfo attachmentInfo;

    private String botAlias; //机器人的别名，custom

    private String botId; // 大模型的id,自己设置

    private long breakingStrategy; // 截断策略

    private List<Capability> capabilities; // 大模型的能力

    private String conversationId;// 会话id,每次调用新对话获取

    private String inputWay; // 标识输入触发方式（如proactive=用户主动发送，auto=系统自动触发）。

    private boolean isNewConversation;// 是否是新对话,默认为是

    private boolean isRetry;// 是否是对这一问题的重新回答,默认为否

    private List<String> mediaInfos;// 用户上传的媒体文件信息（如图片、文档），当前为空表示无附件。

    private String query;// 查询的问题,自己设置

    private String rewriteQuery; // 可能由系统自动填充，存储优化/改写后的用户问题（如纠错、意图澄清）。

    private long turnIndex;// 自己设置,标记当前对话轮次（从0开始递增），用于跟踪多轮交互中的上下文位置,第0表示用户的第一个问题,1表示用户的第二个问题

    private long userId; // 用户id
    @Data
    class AttachmentInfo {
        private URL url;
        AttachmentInfo(){
            url = new URL();
        }
    }

    @Data
    class URL {
        private List<String> infoList;
        URL(){
            infoList = new ArrayList<>();
        }
    }

    @Data
    class Capability {
        private String botDesc; // 开启深度查询描述

        private String botIcon;

        private long botId; // 需要自己设置

        private String capability;

        private long capabilityRang;

        private boolean defaultHidden;

        private String defaultQuery;

        private boolean defaultSelected; // 是否默认选中该功能

        private List<String> exclusiveCapabilities;// 排除功能，排除深度搜索,联网搜索

        private String icon; // 图标

        private long _id;

        private String minAppVersion;

        private String selectedIcon;

        private String title;

        Capability(){
            title = "深度思考(R1)";
            defaultQuery = "";
            capability = "otherBot";
            capabilityRang = 0;
            minAppVersion = "";
            botId = 200004;
            botDesc = "深度回答这个问题（Deepseek R1）";
            exclusiveCapabilities = null;
            defaultSelected = true;
            defaultHidden = false;
            _id = 0;
            icon = "https://wy-static.wenxiaobai.com/tuwen_image/3f3fdf1cba4ab50ead55571f9d05ae4b0eb2a6df9401edede644a2bc66fc6698";
            selectedIcon = "https://wy-static.wenxiaobai.com/tuwen_image/e619ae7984a65e5645cce5db7864670b4b88748d9240664ab4b97cf217c2a4d3";
            botIcon = "https://platform-dev-1319140468.cos.ap-nanjing.myqcloud.com/bot/avatar/2025/02/06/612cbff8-51e6-4c6a-8530-cb551bcfda56.webp";
        }
    }

    public WenXiaoBaiRequestBody(){
        this.userId = 102056182;
        this.botAlias = "custom";
        this.breakingStrategy = 0;
        conversationId = "828b2bce-4923-4354-a111-471b6937fe57";
        capabilities = new ArrayList<>();
        capabilities.add(new Capability());
        mediaInfos = new ArrayList<>();
        rewriteQuery = "";
        inputWay = "proactive";
        attachmentInfo = new AttachmentInfo();
        isNewConversation = true;
        isRetry = false;
    }
}





