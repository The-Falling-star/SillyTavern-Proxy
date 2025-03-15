package com.ling.sillytavernproxy.entity.DeepSeek;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeepSeekRequestBody {
    @JsonProperty("chat_session_id")
    private String chatSessionId;

    @JsonProperty(value = "parent_message_id",required = true)
    private Integer parentMessageId;

    private String prompt;

    @JsonProperty(value = "ref_file_ids")
    private List<String> refFileIds;

    @JsonProperty("search_enabled")
    private boolean searchEnabled;

    @JsonProperty("thinking_enabled")
    private boolean thinkingEnabled;

    public DeepSeekRequestBody() {
        searchEnabled = true;
        thinkingEnabled = false;
        refFileIds = new ArrayList<>();
    }
}
