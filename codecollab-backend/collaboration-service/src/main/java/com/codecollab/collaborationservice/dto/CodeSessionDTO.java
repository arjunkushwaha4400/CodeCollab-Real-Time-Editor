package com.codecollab.collaborationservice.dto;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class CodeSessionDTO {
    private String ownerUsername;
    private Map<String, String> participants;
    private Set<String> blockedUsers;
}