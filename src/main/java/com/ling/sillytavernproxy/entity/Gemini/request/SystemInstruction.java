package com.ling.sillytavernproxy.entity.Gemini.request;

import com.ling.sillytavernproxy.entity.Gemini.Text;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemInstruction {
    private List<Text> parts;
}
