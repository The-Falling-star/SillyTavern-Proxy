package com.ling.sillytavernproxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.enums.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DialogInputDTO {
    private boolean stream;

    private List<Message> messages;

    private Model model;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("char_name")
    private String charName;

    @JsonProperty("n")
    private Integer replyNum;// 每次生成的回复数量

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("top_K")
    private Integer topK;

    @JsonProperty("include_reasoning")
    private Boolean includeReasoning;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;// 频率惩罚

    @JsonProperty("presence_penalty")
    private Double presencePenalty;// 存在惩罚

    @JsonProperty("enable_web_search")
    private Boolean enableWebSearch;

    @JsonProperty("chat_completion_source")
    private String chatCompletionSource;

    @JsonProperty("group_names")
    private List<String> groupNames;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("proxy_password")
    private String proxyPassword;

    @JsonProperty("reasoning_effort")
    private Double reasoningEffort;

    @JsonProperty("request_images")
    private Boolean requestImages;

    @JsonProperty("use_makersuite_sysprompt")
    private Boolean useMakersuiteSysprompt;

    private List<String> stop;

    private Integer seed;

}
