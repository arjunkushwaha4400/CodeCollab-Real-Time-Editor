package com.codecollab.sessionservice.controller;

import com.codecollab.sessionservice.model.Comment;
import com.codecollab.sessionservice.model.CommentThread;
import com.codecollab.sessionservice.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{sessionId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<CommentThread> startThread(
            @PathVariable String sessionId,
            @RequestHeader("X-Authenticated-Username") String author,
            @RequestBody CreateThreadRequest request) {

        CommentThread newThread = sessionService.startCommentThread(
                sessionId,
                author,
                request.getLineNumber(),
                request.getContent()
        );
        return new ResponseEntity<>(newThread, HttpStatus.CREATED);
    }

    @PostMapping("/{threadId}/replies")
    public ResponseEntity<Comment> addReply(
            @PathVariable Long threadId,
            @RequestHeader("X-Authenticated-Username") String author,
            @RequestBody CreateCommentRequest request) {

        Comment newComment = sessionService.addReplyToThread(threadId, author, request.getContent());
        return new ResponseEntity<>(newComment, HttpStatus.CREATED);
    }

    @PutMapping("/{threadId}/resolve")
    public ResponseEntity<CommentThread> resolveThread(
            @PathVariable Long threadId,
            @RequestHeader("X-Authenticated-Username") String resolverUsername) {

        CommentThread resolvedThread = sessionService.resolveCommentThread(threadId, resolverUsername);
        return ResponseEntity.ok(resolvedThread);
    }
}