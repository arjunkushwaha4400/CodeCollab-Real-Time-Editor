package com.codecollab.collaborationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeUpdateMessage {
    private String content;
    // We can add more fields later, like senderId, cursorPosition, etc.
}