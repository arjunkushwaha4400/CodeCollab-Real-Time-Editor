package com.codecollab.collaborationservice.client;

import com.codecollab.collaborationservice.dto.CodeSessionDTO;
import com.codecollab.collaborationservice.dto.CodeUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

// The 'name' must exactly match the spring.application.name of the target service
@FeignClient(name = "SESSION-SERVICE")
public interface SessionServiceClient {

    @PutMapping("/api/sessions/{uniqueId}")
    void updateSessionCode(
            @PathVariable("uniqueId") String uniqueId,
            @RequestBody CodeUpdateRequest request);

    @GetMapping("/api/sessions/{uniqueId}")
    CodeSessionDTO getSessionById(@PathVariable String uniqueId);
}