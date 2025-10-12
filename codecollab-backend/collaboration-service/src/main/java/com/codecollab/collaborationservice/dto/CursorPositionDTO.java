package com.codecollab.collaborationservice.dto;

import lombok.Data;

@Data
public class CursorPositionDTO {
    private String username;
    // We can add a color field later if needed, but for now, the frontend can manage colors.

    // These fields match the structure of the Monaco Editor's selection/position object
    private int startLineNumber;
    private int startColumn;
    private int endLineNumber;
    private int endColumn;
}