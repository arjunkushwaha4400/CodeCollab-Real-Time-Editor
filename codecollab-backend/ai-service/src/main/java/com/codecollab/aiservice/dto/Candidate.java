package com.codecollab.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Candidate(Content content, String finishReason) {
}