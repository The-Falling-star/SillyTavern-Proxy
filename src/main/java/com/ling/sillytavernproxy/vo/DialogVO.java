package com.ling.sillytavernproxy.vo;

import com.ling.sillytavernproxy.vo.reply.ReplyVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DialogVO {
    private List<ReplyVO> choices;
}
