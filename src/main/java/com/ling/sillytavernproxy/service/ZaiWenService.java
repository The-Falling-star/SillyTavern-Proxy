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

@Service("zaiWenService")
@Slf4j
@AllArgsConstructor
public class ZaiWenService implements DialogService {

    @Override
    public Map<String, Object> inputToRequestBody(DialogInputDTO dialogInputDTO) {
        ZaiWenRequestBody requestBody = new ZaiWenRequestBody(dialogInputDTO);
        requestBody.setMode(dialogInputDTO.getModel().getOut());
        return BeanUtil.beanToMap(requestBody);
    }

    @Override
    public DialogVO streamResponseToDialogVO(Integer index, String data) {
        List<ReplyVO> replyVOs = new LinkedList<>();
        Message message = new Message();
        message.setContent(data);
        message.setRole("assistant");
        replyVOs.add(new CommonReplyVO(message,index,data == null || data.isEmpty()));
        return new DialogVO(replyVOs);
    }

    @Override
    public DialogVO notStreamResponseToDialogVO(Integer index, String data) {
        // 因为这个网站只能流式返回数据,因此处理方式和流式数据处理方式一样
        return streamResponseToDialogVO(index,data);
    }

    @Override
    public String getUrl(DialogInputDTO dialogInputDTO) {
        return FinalNumber.ZAI_WEN_URL + "/chatbot";
    }

    @Override
    public boolean isStream(DialogInputDTO dialogInputDTO) {
        return true;
    }
}



