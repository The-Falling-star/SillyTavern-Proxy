package com.ling.sillytavernproxy.entity.Gemini.request;

import com.ling.sillytavernproxy.enums.gemini.HarmBlockThreshold;
import com.ling.sillytavernproxy.enums.gemini.HarmCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SafetySetting {
    private HarmCategory category;
    private HarmBlockThreshold threshold;

}
