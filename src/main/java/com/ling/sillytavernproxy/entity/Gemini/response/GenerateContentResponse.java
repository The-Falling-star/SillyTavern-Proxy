package com.ling.sillytavernproxy.entity.Gemini.response;

import com.ling.sillytavernproxy.enums.gemini.BlockReason;
import com.ling.sillytavernproxy.enums.gemini.HarmCategory;
import com.ling.sillytavernproxy.enums.gemini.HarmProbability;
import lombok.Data;

import java.util.List;

@Data
public class GenerateContentResponse {
    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;
    // private UsageMetadata usageMetadata;
    private String modelVersion;
}

@Data
class SafetyRating{
    private Boolean blocked;
    private HarmCategory category;
    private HarmProbability probability;
}

@Data
class PromptFeedback{
    private List<SafetyRating> safetyRatings;
    private BlockReason blockReason;
}


