package com.codecollab.collaborationservice.controller;

import com.codecollab.collaborationservice.client.SessionServiceClient;
import com.codecollab.collaborationservice.dto.ChatMessage;
import com.codecollab.collaborationservice.dto.CodeUpdateMessage;
import com.codecollab.collaborationservice.dto.CodeUpdateRequest;
import com.codecollab.collaborationservice.dto.CursorPositionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller // Note: We use @Controller, not @RestController for WebSockets
@RequiredArgsConstructor
public class CollaborationController {

    private final SessionServiceClient sessionServiceClient;


    @MessageMapping("/code/{sessionId}")
    @SendTo("/topic/code/{sessionId}")
    public CodeUpdateMessage handleCodeUpdate(
            @DestinationVariable String sessionId,
            CodeUpdateMessage message) {


        System.out.println("Received message for session " + sessionId + ": " + message.getContent());

        sessionServiceClient.updateSessionCode(sessionId, new CodeUpdateRequest(message.getContent()));
        System.out.println("Persisted code update for session " + sessionId);

        return message;
    }

    @MessageMapping("/chat/{sessionId}")
    @SendTo("/topic/chat/{sessionId}")
    public ChatMessage handleChatMessage(@DestinationVariable String sessionId, ChatMessage chatMessage) {
        System.out.println("Received chat message for session " + sessionId + ": " + chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/cursor/{sessionId}")
    @SendTo("/topic/cursor/{sessionId}")
    public CursorPositionDTO handleCursorMove(
            @DestinationVariable String sessionId,
            @Payload CursorPositionDTO cursorPosition,
            SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser().getName();
        cursorPosition.setUsername(username);


        return cursorPosition;
    }




}