package com.codecollab.collaborationservice.security;

import com.codecollab.collaborationservice.client.SessionServiceClient;
import com.codecollab.collaborationservice.dto.CodeSessionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // <-- NEW IMPORT for logging
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.security.Principal;

@Component
@Slf4j // <-- NEW ANNOTATION to add a logger automatically
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final SessionServiceClient sessionServiceClient;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        log.info("--- Interceptor Running for a message ---"); // Log that the interceptor is active

        if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            String destination = accessor.getDestination();

            log.info("Message Type: SEND, Destination: {}", destination);

            if (user != null && user.getName() != null && destination != null) {
                String sessionId = extractSessionId(destination);
                if (sessionId != null) {

                    log.info("Extracted Session ID: {}", sessionId);
                    log.info("Authenticated User Principal: {}", user.getName());

                    // Ask the session-service for the permission details
                    CodeSessionDTO session = sessionServiceClient.getSessionById(sessionId);

                    // --- THIS IS THE CRITICAL LOG ---
                    // It will show us exactly what we are comparing.
                    log.info("PERMISSION CHECK: User='{}', Blocked List='{}'", user.getName(), session.getBlockedUsers());

                    // Security Check: Is the user blocked for this session?
                    if (session.getBlockedUsers().contains(user.getName())) {
                        log.warn("ACCESS DENIED for blocked user '{}' in session '{}'", user.getName(), sessionId);
                        throw new AccessDeniedException("You are blocked from contributing to this session.");
                    }

                    log.info("ACCESS GRANTED for user '{}'", user.getName());
                }
            } else {
                log.warn("Message is SEND type, but user principal or destination is null. Skipping security check.");
            }
        }
        return message;
    }

    private String extractSessionId(String destination) {
        if (destination == null) return null;
        String[] parts = destination.split("/");
        if (parts.length > 2) {
            return parts[parts.length - 1];
        }
        return null;
    }
}