package com.codecollab.collaborationservice.config;

import com.codecollab.collaborationservice.security.AuthChannelInterceptor;
import com.codecollab.collaborationservice.security.CustomHandshakeHandler; // <-- NEW IMPORT
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;

    @PostConstruct
    public void init() {
        System.out.println("=== WebSocket Config Initialized ===");
        System.out.println("CustomHandshakeHandler: " + (customHandshakeHandler != null ? "INJECTED" : "NULL"));
        System.out.println("AuthChannelInterceptor: " + (authChannelInterceptor != null ? "INJECTED" : "NULL"));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("=== Registering STOMP Endpoints ===");

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(customHandshakeHandler)
                .withSockJS();
//                .setSupressCors(true);
        System.out.println("WebSocket endpoint registered with custom handshake handler");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic","/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}