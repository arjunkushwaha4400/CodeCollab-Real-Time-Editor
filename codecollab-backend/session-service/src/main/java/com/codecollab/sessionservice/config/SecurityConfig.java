package com.codecollab.sessionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Since the gateway is handling security, permit all requests within this service
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }
}