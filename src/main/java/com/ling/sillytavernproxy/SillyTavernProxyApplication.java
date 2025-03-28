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
        SpringApplication.run(SillyTavernProxyApplication.class, args);
    }

}
