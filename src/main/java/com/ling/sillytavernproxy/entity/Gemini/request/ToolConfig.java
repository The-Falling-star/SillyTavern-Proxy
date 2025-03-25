package com.ling.sillytavernproxy.entity.Gemini.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class ToolConfig {
    private FunctionCallingConfig functionCallingConfig;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class FunctionCallingConfig {
    private Mode mode;
    private List<String> allowedFunctionNames;
}

enum Mode{
    MODE_UNSPECIFIED,AUTO,ANY,NONE
}
