package com.ling.sillytavernproxy.vo.reply;

import com.ling.sillytavernproxy.entity.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommonReplyVO extends ReplyVO {
    Message message;

    public CommonReplyVO(Message message,Integer index, Boolean error) {
        super(index, error);
        this.message = message;
    }
}
