package com.codecollab.sessionservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp // Automatically set the timestamp when created
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String codeContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_session_id")
    @JsonIgnore // Prevents infinite loops when serializing to JSON
    private CodeSession codeSession;
}