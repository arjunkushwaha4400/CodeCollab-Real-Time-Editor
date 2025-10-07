package com.codecollab.collaborationservice.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Get the username from the header added by our API Gateway
        String username = request.getHeaders().getFirst("X-Authenticated-Username");

        if (username != null && !username.isBlank()) {
            System.out.println("WebSocket handshake successful for user: " + username);
            return new UserPrincipal(username);
        }

        // If no username, assign a random guest ID (or you could reject the connection)
        System.out.println("WebSocket handshake for anonymous user.");
        return new UserPrincipal("guest-" + UUID.randomUUID());
    }
}