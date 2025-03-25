package com.ling.sillytavernproxy.entity.Gemini.response;

import com.ling.sillytavernproxy.entity.Gemini.Content;
import com.ling.sillytavernproxy.enums.gemini.FinishReason;
import lombok.Data;

import java.util.List;

@Data
public class Candidate{
    private Content content;
    private FinishReason finishReason;
    private List<SafetyRating> safetyRatings;
    private Integer tokenCount;
}
