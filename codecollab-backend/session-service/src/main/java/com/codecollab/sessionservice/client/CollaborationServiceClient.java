package com.codecollab.sessionservice.client;

import com.codecollab.sessionservice.dto.NotificationDTO; // Create this DTO
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "collaboration-service", url = "http://localhost:8084")
public interface CollaborationServiceClient {

    @PostMapping("/api/internal/broadcast/notify-owner/{ownerUsername}")
    void notifyOwner(@PathVariable("ownerUsername") String ownerUsername, NotificationDTO notification);

    @PostMapping("/api/internal/broadcast/session/{sessionId}")
    void broadcastToSession(@PathVariable("sessionId") String sessionId, NotificationDTO notification);
}