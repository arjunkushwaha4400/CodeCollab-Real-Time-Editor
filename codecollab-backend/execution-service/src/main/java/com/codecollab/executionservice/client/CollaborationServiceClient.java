package com.codecollab.executionservice.client;


import com.codecollab.executionservice.dto.CodeExecutionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "collaboration-service", url = "http://localhost:8084")
public interface CollaborationServiceClient {


    @PostMapping("/api/internal/broadcast/{sessionId}")
    void broadcastResult(@PathVariable("sessionId")  String sessionId, @RequestBody CodeExecutionResponse result);
}
