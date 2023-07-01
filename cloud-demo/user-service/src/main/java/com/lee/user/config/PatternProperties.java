package com.lee.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author LiJing
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "pattern")
@Data
public class PatternProperties {
    private String dateformat;
    private String envSharedValue;
    private String name;
}
