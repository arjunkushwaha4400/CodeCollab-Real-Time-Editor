package com.codecollab.executionservice.controller;

import com.codecollab.executionservice.client.CollaborationServiceClient;
import com.codecollab.executionservice.dto.CodeExecutionRequest;
import com.codecollab.executionservice.dto.CodeExecutionResponse;
import com.codecollab.executionservice.service.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor

public class ExecutionController {

    private final CodeExecutionService codeExecutionService;
    private final CollaborationServiceClient collaborationServiceClient;
    private static final Logger logger = LoggerFactory.getLogger(ExecutionController.class);


    @PostMapping
    public ResponseEntity<Void> executeCode(@RequestBody CodeExecutionRequest request) {
        try {

            CodeExecutionResponse response = codeExecutionService.executeCode(
                    request.getCode(),
                    request.getLanguage(),
                    request.getStdin() // <-- NEW
            );

            collaborationServiceClient.broadcastResult(
                    request.getSessionId(),
                    new CodeExecutionResponse(response.getOutput(), response.getError())
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            collaborationServiceClient.broadcastResult(
                    request.getSessionId(),
                    new CodeExecutionResponse("", e.getMessage())
            );
            return ResponseEntity.ok().build();

        }
    }
}