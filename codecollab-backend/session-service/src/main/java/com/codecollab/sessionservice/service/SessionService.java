package com.codecollab.sessionservice.service;

import com.codecollab.sessionservice.SessionServiceApplication;
import com.codecollab.sessionservice.client.CollaborationServiceClient;
import com.codecollab.sessionservice.controller.CreateSessionRequest;
import com.codecollab.sessionservice.dto.NotificationDTO;
import com.codecollab.sessionservice.exception.SessionNotFoundException;
import com.codecollab.sessionservice.exception.UnauthorizedException;
import com.codecollab.sessionservice.model.CodeSession;
import com.codecollab.sessionservice.model.Comment;
import com.codecollab.sessionservice.model.CommentStatus;
import com.codecollab.sessionservice.model.CommentThread;
import com.codecollab.sessionservice.model.Role;
import com.codecollab.sessionservice.model.Snapshot;
import com.codecollab.sessionservice.repository.CodeSessionRepository;
import com.codecollab.sessionservice.repository.CommentRepository;
import com.codecollab.sessionservice.repository.CommentThreadRepository;
import com.codecollab.sessionservice.repository.SnapshotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.codecollab.sessionservice.dto.NotificationDTO.NotificationType;

import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final CodeSessionRepository codeSessionRepository;
    private final SnapshotRepository snapshotRepository;
    private final CollaborationServiceClient collaborationServiceClient;
    private final CommentThreadRepository commentThreadRepository;
    private final CommentRepository commentRepository;
    private static final Logger log = LoggerFactory.getLogger(SessionServiceApplication.class);

    @Transactional
    public CodeSession createNewSession(String ownerUsername, CreateSessionRequest request) {
        String boilerplate = getBoilerplateForLanguage(request.getLanguage());

        CodeSession newSession = CodeSession.builder()
                .uniqueId(UUID.randomUUID().toString())
                .codeContent(boilerplate)
                .ownerUsername(ownerUsername)
                .isPrivate(request.isPrivate())
                .language(request.getLanguage())
                .build();

        newSession.getParticipants().put(ownerUsername, Role.OWNER);
        return codeSessionRepository.save(newSession);
    }

    @Transactional
    public void requestToJoin(String uniqueId, String username) {
        CodeSession session = getSessionByUniqueId(uniqueId);

        if (session.isPrivate() && !session.getParticipants().containsKey(username)) {
            session.getPendingRequests().add(username);
            codeSessionRepository.save(session);

            // --- THIS IS THE TRIGGER ---
            // After saving the request, call the collaboration-service to send a notification
            NotificationDTO notification = new NotificationDTO(
                    username,
                    username + " wants to join your session.",
                    NotificationDTO.NotificationType.JOIN_REQUEST
            );
            collaborationServiceClient.notifyOwner(session.getOwnerUsername(), notification);

        } else if (!session.isPrivate()) {
            session.getParticipants().put(username, Role.EDITOR);
            codeSessionRepository.save(session);
        }
    }

    @Transactional
    public CodeSession approveJoinRequest(String uniqueId, String ownerUsername, String userToApprove) {
        CodeSession session = getSessionByUniqueId(uniqueId);

        if (!session.getOwnerUsername().equals(ownerUsername)) {
            throw new UnauthorizedException("Only owner can approve requests");
        }

        session.getPendingRequests().remove(userToApprove);
        session.getParticipants().put(userToApprove, Role.EDITOR);
        codeSessionRepository.save(session);


        NotificationDTO approvalNotification = new NotificationDTO(
                ownerUsername,
                "Your join request has been approved! You can now join the session.",
                NotificationDTO.NotificationType.JOIN_APPROVED
        );

        // THIS IS CRITICAL: Send to the user who requested to join
        collaborationServiceClient.notifyOwner(userToApprove, approvalNotification);

        log.info("User {} approved for session {}. Notification sent to {}",
                userToApprove, uniqueId, userToApprove);

        return session;
    }


    @Transactional
    public CodeSession denyJoinRequest(String uniqueId, String ownerUsername, String userToDeny) {
        CodeSession session = getSessionByUniqueId(uniqueId);

        if (!session.getOwnerUsername().equals(ownerUsername)) {
            throw new UnauthorizedException("Only owner can deny requests");
        }

        session.getPendingRequests().remove(userToDeny);
        codeSessionRepository.save(session);

        // Send notification to the user who was denied
        NotificationDTO denialNotification = new NotificationDTO(
                ownerUsername,
                "Your join request has been denied by the session owner.",
                NotificationDTO.NotificationType.JOIN_DENIED
        );

        // THIS IS CRITICAL: Send to the user who requested to join
        collaborationServiceClient.notifyOwner(userToDeny, denialNotification);

        log.info("User {} denied for session {}. Notification sent to {}",
                userToDeny, uniqueId, userToDeny);

        return session;
    }

    @Transactional
    public CodeSession changeUserRole(String uniqueId, String ownerUsername, String usernameToChange, Role newRole) {
        CodeSession session = getSessionByUniqueId(uniqueId);
        if (!session.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Only the session owner can change roles.");
        }
        if (session.getParticipants().containsKey(usernameToChange)) {
            session.getParticipants().put(usernameToChange, newRole);
            return codeSessionRepository.save(session);
        }
        return session;
    }

    public CodeSession getSessionByUniqueId(String uniqueId) {
        CodeSession session = codeSessionRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with ID: " + uniqueId));

        log.info("Session found - ID: {}, Owner: {}, PendingRequests: {}",
                uniqueId, session.getOwnerUsername(), session.getPendingRequests());
        log.info("PendingRequests size: {}", session.getPendingRequests().size());
        log.info("PendingRequests content: {}", session.getPendingRequests());

        return session;
    }

    public CodeSession updateSessionCode(String uniqueId, String newCode) {
        // First, find the existing session. This will throw an exception if not found.
        CodeSession sessionToUpdate = getSessionByUniqueId(uniqueId);

        // Update the content
        sessionToUpdate.setCodeContent(newCode);

        // Save the updated session back to the database
        return codeSessionRepository.save(sessionToUpdate);
    }

    public CodeSession blockUser(String uniqueId, String ownerUsername, String userToBlock) {
        CodeSession session = getSessionByUniqueId(uniqueId);
        // Security Check: Only the owner can block users.
        if (!session.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Only the session owner can block users.");
        }
        session.getBlockedUsers().add(userToBlock);
        return codeSessionRepository.save(session);
    }

    public CodeSession unblockUser(String uniqueId, String ownerUsername, String userToUnblock) {
        CodeSession session = getSessionByUniqueId(uniqueId);
        // Security Check: Only the owner can unblock users.
        if (!session.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Only the session owner can unblock users.");
        }
        session.getBlockedUsers().remove(userToUnblock);
        return codeSessionRepository.save(session);
    }

    public CodeSession addParticipant(String uniqueId, String username) {
        CodeSession session = getSessionByUniqueId(uniqueId);
        // Security Check: If the user is on the block list, they cannot join.
        if (session.getBlockedUsers().contains(username)) {
            throw new AccessDeniedException("You have been blocked from this session.");
        }
        session.getParticipants().put(username,Role.EDITOR);
        return codeSessionRepository.save(session);
    }

    @Transactional // Ensures the operation is atomic
    public CodeSession saveSnapshot(String uniqueId, String ownerUsername) {
        CodeSession session = getSessionByUniqueId(uniqueId);
        if (!session.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Only the session owner can save snapshots.");
        }

        Snapshot snapshot = Snapshot.builder()
                .codeContent(session.getCodeContent())
                .codeSession(session)
                .build();

        // This is the change: We save the new snapshot directly, which is cleaner
        // and avoids the duplicate-creation issue.
        snapshotRepository.save(snapshot);

        // We still return the session object so the frontend can get the updated list
        return getSessionByUniqueId(uniqueId);
    }

    public CodeSession revertToSnapshot(String uniqueId, String ownerUsername, Long snapshotId) {
        CodeSession session = getSessionByUniqueId(uniqueId);
        // Security Check: Only the owner can revert
        if (!session.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Only the session owner can revert to a snapshot.");
        }

        Snapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found with ID: " + snapshotId));

        // Security check to ensure snapshot belongs to the session
        if (!snapshot.getCodeSession().getId().equals(session.getId())) {
            throw new AccessDeniedException("This snapshot does not belong to the current session.");
        }

        // Update the main code content with the snapshot's content
        session.setCodeContent(snapshot.getCodeContent());
        return codeSessionRepository.save(session);
    }

    @Transactional
    public CodeSession leaveSession(String uniqueId, String username) {
        CodeSession session = getSessionByUniqueId(uniqueId);

        log.info("User {} leaving session {}", username, uniqueId);

        // Remove user from participants (if not owner)
        if (session.getParticipants().containsKey(username) &&
                !session.getOwnerUsername().equals(username)) {

            session.getParticipants().remove(username);
            codeSessionRepository.save(session);

            log.info("User {} removed from session {}", username, uniqueId);

            // Send notification to all participants that user left
            NotificationDTO leaveNotification = new NotificationDTO(
                    username,
                    username + " has left the session.",
                    NotificationDTO.NotificationType.USER_LEFT
            );

            // Notify all participants about user leaving
            collaborationServiceClient.broadcastToSession(uniqueId, leaveNotification);

        } else if (session.getOwnerUsername().equals(username)) {
            log.info("Session owner {} cannot leave, they must delete the session", username);
            throw new UnauthorizedException("Session owner cannot leave. Please delete the session instead.");
        }

        return session;
    }

    // In your SessionService.java
    @Transactional
    public void deleteSession(String uniqueId, String username) {
        CodeSession session = getSessionByUniqueId(uniqueId);

        if (!session.getOwnerUsername().equals(username)) {
            throw new UnauthorizedException("Only the session owner can delete the session");
        }

        log.info("Deleting session {} by owner {}", uniqueId, username);

        // Send notification to all participants that session is being deleted
        NotificationDTO deleteNotification = new NotificationDTO(
                username,
                "The session has been deleted by the owner.",
                NotificationDTO.NotificationType.SESSION_DELETED
        );

        // Notify all participants about session deletion
        collaborationServiceClient.broadcastToSession(uniqueId, deleteNotification);

        // Delete the session from repository
        codeSessionRepository.delete(session);

        log.info("Session {} deleted successfully", uniqueId);
    }

    @Transactional
    public CommentThread startCommentThread(String uniqueId, String author, int lineNumber, String content) {
        CodeSession session = getSessionByUniqueId(uniqueId);
        // Security Check: User must be a participant to comment.
        if (!session.getParticipants().containsKey(author)) {
            throw new AccessDeniedException("Only participants can add comments.");
        }

        CommentThread newThread = CommentThread.builder()
                .lineNumber(lineNumber)
                .status(CommentStatus.OPEN)
                .codeSession(session)
                .build();

        Comment firstComment = Comment.builder()
                .author(author)
                .content(content)
                .commentThread(newThread)
                .build();

        newThread.getComments().add(firstComment);

        return commentThreadRepository.save(newThread);
    }

    @Transactional
    public Comment addReplyToThread(Long threadId, String author, String content) {
        CommentThread thread = commentThreadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Comment thread not found with ID: " + threadId));

        // Security Check: User must be a participant to reply.
        CodeSession session = thread.getCodeSession();
        if (!session.getParticipants().containsKey(author)) {
            throw new AccessDeniedException("Only participants can reply to comments.");
        }

        Comment newComment = Comment.builder()
                .author(author)
                .content(content)
                .commentThread(thread)
                .build();

        return commentRepository.save(newComment);
    }

    @Transactional
    public CommentThread resolveCommentThread(Long threadId, String resolverUsername) {
        CommentThread thread = commentThreadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Comment thread not found with ID: " + threadId));

        // Security Check: Only the session owner can resolve threads.
        if (!thread.getCodeSession().getOwnerUsername().equals(resolverUsername)) {
            throw new AccessDeniedException("Only the session owner can resolve comment threads.");
        }

        thread.setStatus(CommentStatus.RESOLVED);
        return commentThreadRepository.save(thread);
    }

    private String getBoilerplateForLanguage(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> """
                    public class Main {
                        public static void main(String[] args) {
                            System.out.println("Hello, Java!");
                        }
                    }
                    """;
            case "python" -> """
                    def main():
                        print("Hello, Python!")

                    if __name__ == "__main__":
                        main()
                    """;
            case "javascript" -> """
                    function main() {
                        console.log("Hello, JavaScript!");
                    }

                    main();
                    """;
            default -> "// Welcome to your new CodeCollab session!";
        };


    }


}