package com.codecollab.aiservice.controller;

import com.codecollab.aiservice.dto.Part;
import com.codecollab.aiservice.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/explain")
    public ResponseEntity<String> explainCode(@RequestBody Part request) {
        try {

            if (request.text() == null || request.text().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Code text cannot be empty");
            }

            String explanation = aiService.getCodeExplanation(request.text());
            return ResponseEntity.ok(explanation);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error processing request: " + e.getMessage());
        }
    }

    // Test endpoint to check if AI service is working
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testAI() {
        String testCode = """
        public class HelloWorld {
            public static void main(String[] args) {
                System.out.println("Hello, World!");
            }
        }
        """;

        try {
            String explanation = aiService.getCodeExplanation(testCode);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "explanation", explanation
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

}