package com.ling.sillytavernproxy.service;

import cn.hutool.core.bean.BeanUtil;
import com.ling.sillytavernproxy.config.FinalNumber;
import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.ZaiWen.ZaiWenRequestBody;
import com.ling.sillytavernproxy.entity.Message;
import com.ling.sillytavernproxy.vo.DialogVO;
import com.ling.sillytavernproxy.vo.reply.CommonReplyVO;
import com.ling.sillytavernproxy.vo.reply.ReplyVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class ZaiWenService implements DialogService {

    Map<String,String> models;

    @Override
    public Map<String, ?> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        ZaiWenRequestBody requestBody = new ZaiWenRequestBody(dialogInputDTO);
        requestBody.setMode(models.get(dialogInputDTO.getModel()));
        return BeanUtil.beanToMap(requestBody);
    }

    @Override
    public DialogVO streamResponseToDialogVO(Integer index, String data) {
        log.info("第{}对话:{}",index,data);
        List<ReplyVO> replyVOs = new LinkedList<>();
        Message message = new Message();
        message.setContent(data);
        message.setRole("assistant");
        replyVOs.add(new CommonReplyVO(message,index,data == null || data.isEmpty()));
        return new DialogVO(replyVOs);
    }

    @Override
    public DialogVO notStreamResponseToDialogVO(Integer index, String data) {
        return streamResponseToDialogVO(index,data);
    }

    @Override
    public String getUrl() {
        return FinalNumber.ZAI_WEN_URL + "/chatbot";
    }

    @Override
    public boolean isStream(DialogInputDTO dialogInputDTO) {
        return true;
    }
}
