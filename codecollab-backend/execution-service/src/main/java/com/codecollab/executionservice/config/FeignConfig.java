package com.codecollab.executionservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.codecollab.executionservice.client")
public class FeignConfig {
}
