package com.ling.sillytavernproxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ling.sillytavernproxy.entity.Message;
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

    private String model;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("char_name")
    private String charName;

    @JsonProperty("n")
    private Integer replyNum;// 每次生成的回复数量

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;// 频率惩罚

    @JsonProperty("presence_penalty")
    private Double presencePenalty;// 存在惩罚

}
