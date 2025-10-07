package com.codecollab.sessionservice.repository;

import com.codecollab.sessionservice.model.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
}