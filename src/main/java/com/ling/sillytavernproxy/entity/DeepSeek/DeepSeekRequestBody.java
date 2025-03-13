package com.ling.sillytavernproxy.entity.DeepSeek;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeepSeekRequestBody {
    @JsonProperty("chat_session_id")
    private String chatSessionId;

    @JsonProperty("parent_message_id")
    private Integer parentMessageId;

    private String prompt;

    @JsonProperty("ref_file_ids")
    private List<String> refFileIds;

    @JsonProperty("search_enabled")
    private boolean searchEnabled;

    @JsonProperty("thinking_enabled")
    private boolean thinkingEnabled;

    public DeepSeekRequestBody() {
        searchEnabled = true;
        thinkingEnabled = false;
    }
}
