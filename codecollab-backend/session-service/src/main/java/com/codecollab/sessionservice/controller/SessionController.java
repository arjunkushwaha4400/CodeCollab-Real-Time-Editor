package com.codecollab.sessionservice.controller;

import com.codecollab.sessionservice.dto.CodeUpdateRequest;
import com.codecollab.sessionservice.model.CodeSession;
import com.codecollab.sessionservice.model.Role;
import com.codecollab.sessionservice.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<CodeSession> createSession(
            @RequestHeader("X-Authenticated-Username") String username,
            @RequestBody CreateSessionRequest request) {
        CodeSession createdSession = sessionService.createNewSession(username, request);
        return ResponseEntity.status(201).body(createdSession);
    }

    @PostMapping("/{uniqueId}/request-join")
    public ResponseEntity<Void> requestToJoin(
            @PathVariable String uniqueId,
            @RequestHeader("X-Authenticated-Username") String username) {
        sessionService.requestToJoin(uniqueId, username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{uniqueId}/approve/{userToApprove}")
    public ResponseEntity<CodeSession> approveJoinRequest(
            @PathVariable String uniqueId,
            @PathVariable String userToApprove,
            @RequestHeader("X-Authenticated-Username") String ownerUsername) {
        return ResponseEntity.ok(sessionService.approveJoinRequest(uniqueId, ownerUsername, userToApprove));
    }

    @PostMapping("/{uniqueId}/deny/{userToDeny}")
    public ResponseEntity<CodeSession> denyJoinRequest(
            @PathVariable String uniqueId,
            @PathVariable String userToDeny,
            @RequestHeader("X-Authenticated-Username") String ownerUsername) {
        return ResponseEntity.ok(sessionService.denyJoinRequest(uniqueId, ownerUsername, userToDeny));
    }

    @GetMapping("/{uniqueId}")
    public ResponseEntity<CodeSession> getSession(@PathVariable String uniqueId) {

        CodeSession session = sessionService.getSessionByUniqueId(uniqueId);
        if (session.getPendingRequests() == null) {
            session.setPendingRequests(new HashSet<>());
        }
        if (session.getParticipants() == null) {
            session.setParticipants(new HashMap<>());
        }
        if (session.getBlockedUsers() == null) {
            session.setBlockedUsers(new HashSet<>());
        }
        return ResponseEntity.ok(session);
    }

    @PutMapping("/{uniqueId}")
    public ResponseEntity<CodeSession> updateSession(
            @PathVariable String uniqueId,
            @RequestBody CodeUpdateRequest request) {

        CodeSession updatedSession = sessionService.updateSessionCode(uniqueId, request.getCodeContent());
        return ResponseEntity.ok(updatedSession);
    }

    @PostMapping("/{uniqueId}/block/{usernameToBlock}")
    public ResponseEntity<CodeSession> blockUser(
            @PathVariable String uniqueId,
            @PathVariable String usernameToBlock,
            @RequestHeader("X-Authenticated-Username") String ownerUsername) {

        CodeSession updatedSession = sessionService.blockUser(uniqueId, ownerUsername, usernameToBlock);
        return ResponseEntity.ok(updatedSession);
    }

    @DeleteMapping("/{uniqueId}/block/{usernameToUnblock}")
    public ResponseEntity<CodeSession> unblockUser(
            @PathVariable String uniqueId,
            @PathVariable String usernameToUnblock,
            @RequestHeader("X-Authenticated-Username") String ownerUsername) {

        CodeSession updatedSession = sessionService.unblockUser(uniqueId, ownerUsername, usernameToUnblock);
        return ResponseEntity.ok(updatedSession);
    }

    @PostMapping("/{uniqueId}/participants")
    public ResponseEntity<CodeSession> joinSession(
            @PathVariable String uniqueId,
            @RequestHeader("X-Authenticated-Username") String username) {

        CodeSession updatedSession = sessionService.addParticipant(uniqueId, username);
        return ResponseEntity.ok(updatedSession);
    }

    @PostMapping("/{uniqueId}/snapshots")
    public ResponseEntity<CodeSession> saveSnapshot(
            @PathVariable String uniqueId,
            @RequestHeader("X-Authenticated-Username") String ownerUsername) {

        return ResponseEntity.ok(sessionService.saveSnapshot(uniqueId, ownerUsername));
    }

    @PostMapping("/{uniqueId}/revert/{snapshotId}")
    public ResponseEntity<CodeSession> revertToSnapshot(
            @PathVariable String uniqueId,
            @PathVariable Long snapshotId,
            @RequestHeader("X-Authenticated-Username") String ownerUsername) {

        return ResponseEntity.ok(sessionService.revertToSnapshot(uniqueId, ownerUsername, snapshotId));
    }

    @PutMapping("/{uniqueId}/permissions/{username}")
    public ResponseEntity<CodeSession> changeUserRole(
            @PathVariable String uniqueId,
            @PathVariable String username,
            @RequestBody Map<String, Role> roleRequest,
            @RequestHeader("X-Authenticated-Username") String ownerUsername) {

        Role newRole = roleRequest.get("role");
        return ResponseEntity.ok(sessionService.changeUserRole(uniqueId, ownerUsername, username, newRole));
    }

    @PostMapping("/{uniqueId}/leave")
    public ResponseEntity<CodeSession> leaveSession(
            @PathVariable String uniqueId,
            @RequestHeader("X-Authenticated-Username") String username) {

        return ResponseEntity.ok(sessionService.leaveSession(uniqueId, username));
    }

    @DeleteMapping("/{uniqueId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String uniqueId,
            @RequestHeader("X-Authenticated-Username") String username) {

        sessionService.deleteSession(uniqueId, username);
        return ResponseEntity.ok().build();
    }
}