package com.ling.sillytavernproxy.controller;

import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.CommonResponse;
import com.ling.sillytavernproxy.entity.Model;
import com.ling.sillytavernproxy.service.DialogService;
import com.ling.sillytavernproxy.vo.DialogVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.LinkedList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/v1")
@AllArgsConstructor
public class DialogController {

    @Qualifier("zaiWenService")
    private DialogService zaiWenService;

    @Qualifier("wenXiaoBaiService")
    private DialogService wenXiaoBaiService;

    @Qualifier("deepSeekService")
    private DialogService deepSeekService;

    /**
     * 检查连接状态
     * @return 返回可用模型
     */
    @GetMapping("/models")
    public CommonResponse status(){
        List<Model> models = new LinkedList<>();
        models.add(new Model("zaiWen-deepseek-reasoner"));
        models.add(new Model("wenXiaoBai-deepseek"));
        models.add(new Model("deepSeekR1"));
        return new CommonResponse(models);
    }

    @PostMapping(value = "/chat/completions")
    public Flux<DialogVO> generates(@RequestBody DialogInputDTO dialogInputDTO, ServerHttpResponse serverHttpResponse){
        if(dialogInputDTO.isStream()) serverHttpResponse.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
        else serverHttpResponse.getHeaders().setContentType(MediaType.APPLICATION_NDJSON);

        if(dialogInputDTO.getModel().contains("zaiWen")) return zaiWenService.sendDialog(dialogInputDTO);
        if(dialogInputDTO.getModel().contains("wenXiaoBai")) return wenXiaoBaiService.sendDialog(dialogInputDTO);
        if (dialogInputDTO.getModel().contains("deepSeek")) return deepSeekService.sendDialog(dialogInputDTO);
        throw new RuntimeException("未知模型");
    }


}
