package com.lee.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author LiJing
 * @version 1.0
 */
@Configuration
public class MyConfig {
    /**
     * 注册一个RestTemplate的实例到Spring容器
     * 用来发起http请求，实现远程调用
     *
     * @return
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
