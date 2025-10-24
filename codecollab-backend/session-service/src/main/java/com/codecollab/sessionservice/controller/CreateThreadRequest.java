package com.codecollab.sessionservice.controller;

import lombok.Data;

@Data
public class CreateThreadRequest {
    private int lineNumber;
    private String content;
}