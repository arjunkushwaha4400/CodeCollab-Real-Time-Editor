package com.codecollab.sessionservice.repository;

import com.codecollab.sessionservice.model.CommentThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentThreadRepository extends JpaRepository<CommentThread, Long> {
}