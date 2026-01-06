package com.mareliart.memory.repository;

import com.mareliart.memory.model.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    @Query("SELECT m FROM Message m WHERE m.sessionId = :sessionId ORDER BY m.createdAt ASC")
    List<Message> findRecentBySessionIdOrderByCreatedAtAsc(String sessionId, int limit);
}
