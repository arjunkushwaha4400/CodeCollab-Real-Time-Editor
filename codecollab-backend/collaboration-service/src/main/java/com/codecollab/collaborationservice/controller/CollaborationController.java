package com.codecollab.collaborationservice.controller;

import com.codecollab.collaborationservice.client.SessionServiceClient;
import com.codecollab.collaborationservice.dto.ChatMessage;
import com.codecollab.collaborationservice.dto.CodeUpdateMessage;
import com.codecollab.collaborationservice.dto.CodeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller // Note: We use @Controller, not @RestController for WebSockets
@RequiredArgsConstructor
public class CollaborationController {

    private final SessionServiceClient sessionServiceClient;


    @MessageMapping("/code/{sessionId}")
    @SendTo("/topic/code/{sessionId}") // The return value is broadcast to all subscribers of this topic.
    public CodeUpdateMessage handleCodeUpdate(
            @DestinationVariable String sessionId,
            CodeUpdateMessage message) {

        // For now, we are just echoing the message back to all clients in the same session.
        // In the future, we could add logic here to save the code, check for conflicts, etc.
        System.out.println("Received message for session " + sessionId + ": " + message.getContent());

        // Now, call the session-service to persist the code update.
        // Feign makes this look like a simple local method call!
        sessionServiceClient.updateSessionCode(sessionId, new CodeUpdateRequest(message.getContent()));
        System.out.println("Persisted code update for session " + sessionId);

        return message;
    }

    @MessageMapping("/chat/{sessionId}")
    @SendTo("/topic/chat/{sessionId}")
    public ChatMessage handleChatMessage(@DestinationVariable String sessionId, ChatMessage chatMessage) {
        System.out.println("Received chat message for session " + sessionId + ": " + chatMessage.getContent());
        return chatMessage; // Echo the message to all clients in the chat topic
    }




}