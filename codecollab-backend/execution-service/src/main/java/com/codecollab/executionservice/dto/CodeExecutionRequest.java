package com.codecollab.executionservice.dto;

// No Lombok - Plain Old Java Object (POJO)
public class CodeExecutionRequest {
    private String language;
    private String code;
    private String sessionId;
    private String stdin;

    // Getters
    public String getLanguage() {
        return language;
    }

    public String getStdin(){
        return stdin;
    }

    public String getCode() {
        return code;
    }

    public String getSessionId() {
        return sessionId;
    }

    // Setters
    public void setLanguage(String language) {
        this.language = language;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }
}