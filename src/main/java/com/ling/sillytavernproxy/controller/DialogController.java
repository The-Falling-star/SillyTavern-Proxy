package com.ling.sillytavernproxy.controller;

import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.entity.CommonResponse;
import com.ling.sillytavernproxy.enums.Model;
import com.ling.sillytavernproxy.exception.UnknownModelException;
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
        List<Map<String,Model>> models = new LinkedList<>();
        for (Model model : Model.values()) {
            models.add(Map.of("id",model));
        }
        return new CommonResponse(models);
    }

    @PostMapping(value = "/chat/completions")
    public Flux<DialogVO> generates(@RequestBody DialogInputDTO dialogInputDTO, ServerHttpResponse serverHttpResponse){
        if(dialogInputDTO.isStream()) serverHttpResponse.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
        else serverHttpResponse.getHeaders().setContentType(MediaType.APPLICATION_NDJSON);

        return switch (dialogInputDTO.getModel()){
            case UNKNOWN -> Flux.error(new UnknownModelException());
            case ZAI_WEN_DEEPSEEK -> dialogService.get("zaiWenService").sendDialog(dialogInputDTO);
            case DEEPSEEK -> dialogService.get("deepSeekService").sendDialog(dialogInputDTO);
            default -> {
                if (dialogInputDTO.getModel().getId().contains("gemini"))
                    yield dialogService.get("geminiService").sendDialog(dialogInputDTO);
                else yield Flux.error(new UnknownModelException());
            }
        };
    }


}
