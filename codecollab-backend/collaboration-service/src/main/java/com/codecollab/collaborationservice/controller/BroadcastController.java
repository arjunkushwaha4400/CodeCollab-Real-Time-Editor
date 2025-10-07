package com.codecollab.collaborationservice.controller;

import com.codecollab.collaborationservice.dto.CodeExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import com.codecollab.collaborationservice.dto.NotificationDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class BroadcastController {

    // This is a Spring helper for sending WebSocket messages
    private final SimpMessageSendingOperations messagingTemplate;

    @PostMapping("/broadcast/{sessionId}")
    public void broadcastExecutionResult(@PathVariable String sessionId, @RequestBody CodeExecutionResponse result) {
        System.out.println("Broadcasting result to session: " + sessionId);
        // Send the result to the output topic for this session
        messagingTemplate.convertAndSend("/topic/output/" + sessionId, result);
    }

    @PostMapping("/broadcast/notify-owner/{ownerUsername}")
    public void notifyOwner(
            @PathVariable String ownerUsername,
            @RequestBody NotificationDTO notification) {

        log.info("Sending private notification to user: {}", ownerUsername);


        messagingTemplate.convertAndSendToUser(
                ownerUsername,
                "/queue/notifications",
                notification
        );
    }

    @PostMapping("/broadcast/session/{sessionId}")
    public void broadcastToSession(
            @PathVariable String sessionId,
            @RequestBody NotificationDTO notification) {

        log.info("Broadcasting to session {}: {}", sessionId, notification);

        messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId,
                notification
        );
    }


}