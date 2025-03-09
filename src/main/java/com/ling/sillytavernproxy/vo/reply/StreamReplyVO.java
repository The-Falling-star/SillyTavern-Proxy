package com.ling.sillytavernproxy.vo.reply;

import com.ling.sillytavernproxy.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamReplyVO extends ReplyVO {
    Message delta;
    public StreamReplyVO(Message message,Integer index, Boolean error) {
        super(index, error);
        this.delta = message;
    }
}

