package com.codecollab.sessionservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.*;

@Entity
@Table(name = "code_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uniqueId; // This will be the user-facing ID for the session URL

    @Column(columnDefinition = "TEXT") // Use TEXT type for potentially large code blocks
    private String codeContent;

    private String ownerUsername;

    @JsonProperty("isPrivate")
    private boolean isPrivate;

    private String language;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_participants")
    @MapKeyColumn(name = "username")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Map<String, Role> participants = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_pending_requests")
    @Builder.Default
    private Set<String> pendingRequests = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private Set<String> blockedUsers = new HashSet<>();

    @OneToMany(mappedBy = "codeSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp DESC") // Show the newest snapshots first
    @Builder.Default
    private List<Snapshot> history = new ArrayList<>();

    public CodeSession(Object o, String uniqueId, String s, String ownerUsername) {

        this.uniqueId = uniqueId;
        this.codeContent = s;
        this.ownerUsername = ownerUsername;
    }

}