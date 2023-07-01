package com.lee.feign.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

/**
 * @author LiJing
 * @version 1.0
 */
public class DefaultFeignConfiguration {
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.BASIC; // 日志级别为BASIC
    }
}
