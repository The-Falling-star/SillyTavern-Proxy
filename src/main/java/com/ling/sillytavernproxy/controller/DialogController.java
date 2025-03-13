package com.ling.sillytavernproxy.controller;

import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.CommonResponse;
import com.ling.sillytavernproxy.entity.Model;
import com.ling.sillytavernproxy.service.DialogService;
import com.ling.sillytavernproxy.vo.DialogVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.ling.sillytavernproxy.config.FinalNumber.*;

@RestController
@Slf4j
@RequestMapping("/v1")
@AllArgsConstructor
public class DialogController {

    private Map<String,DialogService> dialogService;

    /**
     * 检查连接状态
     * @return 返回可用模型
     */
    @GetMapping("/models")
    public CommonResponse status(){
        List<Model> models = new LinkedList<>();
        models.add(new Model(MODEL_ZAI_WEN_DEEPSEEK));
        models.add(new Model(MODEL_WEN_XIAO_BAI_DEEPSEEK));
        models.add(new Model(MODEL_DEEPSEEK));
        return new CommonResponse(models);
    }

    @PostMapping(value = "/chat/completions")
    public Flux<DialogVO> generates(@RequestBody DialogInputDTO dialogInputDTO, ServerHttpResponse serverHttpResponse){
        if(dialogInputDTO.isStream()) serverHttpResponse.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
        else serverHttpResponse.getHeaders().setContentType(MediaType.APPLICATION_NDJSON);

        return switch (dialogInputDTO.getModel()){
            case MODEL_ZAI_WEN_DEEPSEEK -> dialogService.get("zaiWenService").sendDialog(dialogInputDTO);
            case MODEL_WEN_XIAO_BAI_DEEPSEEK -> dialogService.get("wenXiaoBaiService").sendDialog(dialogInputDTO);
            case MODEL_DEEPSEEK -> dialogService.get("deepSeekService").sendDialog(dialogInputDTO);
            default -> throw new RuntimeException("未知模型");
        };
    }


}
