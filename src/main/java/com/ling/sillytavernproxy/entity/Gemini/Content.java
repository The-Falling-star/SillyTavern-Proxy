package com.ling.sillytavernproxy.entity.Gemini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    private String role;
    private List<Text> parts;

    public Content(String role, String text){
        this.role = role;
        parts = new ArrayList<>();
        parts.add(new Text(text));
    }
}

