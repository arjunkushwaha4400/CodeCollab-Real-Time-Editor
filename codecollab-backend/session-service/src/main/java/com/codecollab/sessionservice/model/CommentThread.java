package com.codecollab.sessionservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CommentThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int lineNumber;

    @Enumerated(EnumType.STRING)
    private CommentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private CodeSession codeSession;

    @OneToMany(mappedBy = "commentThread", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC") // Show oldest comments first
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}