package com.codecollab.sessionservice.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateSessionRequest {
    @JsonProperty("isPrivate")
    private boolean isPrivate;
}