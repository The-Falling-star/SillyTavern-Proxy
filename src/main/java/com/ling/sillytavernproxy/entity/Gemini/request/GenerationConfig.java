package com.ling.sillytavernproxy.entity.Gemini.request;

import com.ling.sillytavernproxy.dto.DialogInputDTO;
import com.ling.sillytavernproxy.enums.Model;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GenerationConfig {
    // 常用的
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxOutputTokens;
    private Integer candidateCount;// 要返回的生成的回答数量

    private List<String> stopSequences;
    private String responseMimeType;
    private FunctionDeclaration.Schema responseSchema;
    private List<Modality> responseModalities;

    private Integer seed;
    private Double presencePenalty; // 存在惩罚
    private Double frequencyPenalty;// 频率惩罚
    private boolean responseLogprobs;
    private Integer logprobs;
    private boolean enableEnhancedCivicAnswers;

    public static final Model[] disablePenaltyModels = {Model.GEMINI_2__5_PRO_EXP,Model.GEMINI_2_FLASH_EXP};

    public GenerationConfig(DialogInputDTO dialogInputDTO){
        this.temperature = dialogInputDTO.getTemperature();
        this.topP = dialogInputDTO.getTopP();
        this.candidateCount = dialogInputDTO.getReplyNum() == null || dialogInputDTO.getReplyNum() < 1 ?
                1 : dialogInputDTO.getReplyNum();
        this.topK = dialogInputDTO.getTopK();
        this.seed = dialogInputDTO.getSeed();
        if (enablePenalty(dialogInputDTO)){
            this.frequencyPenalty = dialogInputDTO.getFrequencyPenalty();
            this.presencePenalty = dialogInputDTO.getPresencePenalty();
        }
        this.maxOutputTokens = dialogInputDTO.getMaxTokens();
        this.stopSequences = dialogInputDTO.getStop();
    }

    private boolean enablePenalty(DialogInputDTO dialogInputDTO){
        for (Model disablePenaltyModel : disablePenaltyModels) {
            if (dialogInputDTO.getModel() == disablePenaltyModel) return false;
        }
        return true;
    }

}

enum Modality{
    MODALITY_UNSPECIFIED,
    TEXT,
    IMAGE,
    AUDIO
}
