package com.ling.sillytavernproxy;

import com.ling.sillytavernproxy.config.FinalNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@SpringBootApplication
public class SillyTavernProxyApplication {

    public static void main(String[] args) {
        // 1. 创建YAML解析器
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        // 2. 加载配置文件（路径不需要前缀，默认从classpath根目录开始）
        factory.setResources(new ClassPathResource("application.yml"));
        // 3. 获取Properties对象
        Properties props = factory.getObject();
        // 4. 读取配置项（支持嵌套格式，如 "server.port"）
        Integer userId = (Integer) props.get("wen-xiao-bai.user-id");
        if (userId == null) {
            throw new RuntimeException("WenXiaoBai.userId is null");
        }
        FinalNumber.XIAO_BAI_USERID = userId;
        String token;
        int i = 0;
        while((token = (String) props.get("wen-xiao-bai.tokens[" + i++ + "]")) != null)
            FinalNumber.XIAO_BAI_TOKENS.add(token);


        SpringApplication.run(SillyTavernProxyApplication.class, args);
    }

}
