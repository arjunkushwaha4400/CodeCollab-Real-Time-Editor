package com.codecollab.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${google.gemini.api-key}")
    private String apiKey;

    @Value("${google.gemini.model:gemini-2.0-flash-001}")
    private String model;

    public String getCodeExplanation(String code) {
        System.out.println("A === AI Service Starting ===");
        System.out.println("API Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "NULL"));
        System.out.println(" Model: " + model);

        String prompt = "Explain this Java code in simple terms for a beginner: \n\n" + code;

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://generativelanguage.googleapis.com")
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // Build request body
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.2,  // Lower temperature for more consistent results
                            "topP", 0.8,
                            "topK", 40,
                            "maxOutputTokens", 1024
                    )
            );

            System.out.println(" Sending request to model: " + model);

            String response = webClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(r -> System.out.println("Received successful response"))
                    .doOnError(e -> System.err.println(" Request failed: " + e.getMessage()))
                    .block();

            if (response == null) {
                return "No response received from AI service";
            }

            System.out.println(" Raw response length: " + response.length());

            // Log first 300 chars of response for debugging
            if (response.length() > 300) {
                System.out.println(" Response preview: " + response.substring(0, 300) + "...");
            } else {
                System.out.println("Full response: " + response);
            }

            return extractTextFromResponse(response);

        } catch (WebClientResponseException e) {
            System.err.println("API Error: " + e.getStatusCode());
            System.err.println(" Error Body: " + e.getResponseBodyAsString());
            return "API Error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();

        } catch (Exception e) {
            System.err.println(" Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return "Unexpected error: " + e.getMessage();
        }
    }

    private String extractTextFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "Empty response from AI service";
        }

        if (response.contains("\"error\"")) {
            return "API Error in response: " + response;
        }

        try {
            System.out.println(" Parsing response...");

            // Method 1: Direct JSON path extraction
            if (response.contains("\"text\":\"")) {
                int start = response.indexOf("\"text\":\"") + 8;
                int end = response.indexOf("\"", start);
                if (end > start) {
                    String text = response.substring(start, end);
                    System.out.println("Successfully extracted text using method 1");
                    return cleanText(text);
                }
            }

            // Method 2: Alternative JSON path
            if (response.contains("\"parts\":[{\"text\":\"")) {
                int start = response.indexOf("\"parts\":[{\"text\":\"") + 18;
                int end = response.indexOf("\"", start);
                if (end > start) {
                    String text = response.substring(start, end);
                    System.out.println(" Successfully extracted text using method 2");
                    return cleanText(text);
                }
            }

            // Method 3: Using Jackson ObjectMapper (most reliable)
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(response);

                // Navigate through the JSON structure: candidates → content → parts → text
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode content = firstCandidate.path("content");
                    JsonNode parts = content.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        String text = firstPart.path("text").asText();
                        if (!text.isEmpty()) {
                            System.out.println(" Successfully extracted text using ObjectMapper");
                            return cleanText(text);
                        }
                    }
                }
            } catch (Exception jsonError) {
                System.err.println(" JSON parsing failed: " + jsonError.getMessage());
            }

            System.out.println(" All extraction methods failed");
            return " Could not extract text from response. Raw response structure: " +
                    response.substring(0, Math.min(200, response.length()));

        } catch (Exception e) {
            System.err.println(" Extraction error: " + e.getMessage());
            return " Extraction error: " + e.getMessage();
        }
    }

    private String cleanText(String text) {
        return text.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\t", "\t")
                .replace("\\r", "")
                .trim();
    }

    // Test method to verify API connectivity
    public String testConnection() {
        try {
            WebClient webClient = WebClient.create();
            String response = webClient.get()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models?key={key}", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return "Connection successful! Available models listed in console.";
        } catch (Exception e) {
            return " Connection failed: " + e.getMessage();
        }
    }
}