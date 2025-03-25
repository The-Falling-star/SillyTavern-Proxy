package com.ling.sillytavernproxy.entity.Gemini.request;

import com.ling.sillytavernproxy.entity.Gemini.Content;
import com.ling.sillytavernproxy.enums.gemini.HarmBlockThreshold;
import com.ling.sillytavernproxy.enums.gemini.HarmCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class GeminiRequestBody {
    private List<Content> contents;

    private GenerationConfig generationConfig;

    private SystemInstruction system_instruction;

    private List<Tool> tools;

    private List<ToolConfig> toolConfig;

    private List<SafetySetting> safetySettings;

    private String cachedContent;

    private List<SafetySetting> getDefaultSafetySetting(){
        List<SafetySetting> settings = new ArrayList<>();
        for (HarmCategory harmCategory : HarmCategory.values()){
            settings.add(new SafetySetting(harmCategory, HarmBlockThreshold.OFF));
        }
        return settings;
    }

    public GeminiRequestBody(){
        this.safetySettings = getDefaultSafetySetting();
    }

}

