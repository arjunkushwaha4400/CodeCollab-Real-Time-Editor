package com.codecollab.userservice.controller;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}