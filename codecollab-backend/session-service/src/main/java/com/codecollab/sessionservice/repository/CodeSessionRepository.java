package com.codecollab.sessionservice.repository;

import com.codecollab.sessionservice.model.CodeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeSessionRepository extends JpaRepository<CodeSession, Long> {
    Optional<CodeSession> findByUniqueId(String uniqueId);
}