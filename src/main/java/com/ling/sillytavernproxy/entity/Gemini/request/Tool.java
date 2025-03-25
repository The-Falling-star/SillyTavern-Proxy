package com.ling.sillytavernproxy.entity.Gemini.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ling.sillytavernproxy.enums.gemini.Type;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Tool {
    private List<FunctionDeclaration> functionDeclarations;
    private GoogleSearchRetrieval googleSearchRetrieval;
}

@Data
class FunctionDeclaration{
    private String name;
    private String description;
    private Schema parameters;
    private Schema response;

    @Data
    static class Schema{
        private Type type;
        private String format;
        private String description;
        private boolean nullable;
        @JsonProperty("enum")
        private List<String> enums;
        private String maxItems;
        private String minItems;
        private Map<String,Schema> properties;
        private List<String> required;
        private List<String> propertyOrdering;
        private Schema items;
    }
}

@Data
class GoogleSearchRetrieval{
    private DynamicRetrievalConfig dynamicRetrievalConfig;
    @Data
    static class DynamicRetrievalConfig{
        private Mode mode;
        private Integer dynamicThreshold;

        enum Mode{
            MODE_UNSPECIFIED,MODE_DYNAMIC
        }
    }
}
