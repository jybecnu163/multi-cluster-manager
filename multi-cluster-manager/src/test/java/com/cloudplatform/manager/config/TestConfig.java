package com.cloudplatform.manager.config;

import com.cloudplatform.manager.util.PasswordEncoder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 注意：这里返回自定义 PasswordEncoder，不是 Spring 接口
        return new PasswordEncoder();
    }
}