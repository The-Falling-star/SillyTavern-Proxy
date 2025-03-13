package com.ling.sillytavernproxy.entity.DeepSeek;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PowChallenge {

    private String algorithm;

    private String challenge;

    private long difficulty;

    private long expireAfter;

    private long expireAt;

    private String salt;

    private String signature;

    private String targetPath;
}
