package com.codecollab.collaborationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String fromUser;
    private String message;
    private NotificationType type;

    public enum NotificationType {
        JOIN_REQUEST,
        JOIN_APPROVED,
        JOIN_DENIED,
        USER_LEFT,
        SESSION_DELETED
    }
}