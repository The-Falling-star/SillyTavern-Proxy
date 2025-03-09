package com.ling.sillytavernproxy.entity.ZaiWen;

import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.Message;
import lombok.Data;

import java.util.List;

@Data
public class ZaiWenRequestBody {
    private List<Message> message;
    private String mode;
    private String prompt_id;
    private String key;

    public ZaiWenRequestBody(DialogInputDTO inputDTO) {
        this.message = inputDTO.getMessages();
        this.prompt_id = "";
        key = null;
    }
}
