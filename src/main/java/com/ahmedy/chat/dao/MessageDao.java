package com.ahmedy.chat.dao;

import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.Message;
import com.ahmedy.chat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

public interface MessageDao extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId);

    @Query(value = """
    SELECT rn - 1
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (ORDER BY sent_at ASC) AS rn
        FROM chat_app.messages
        WHERE conversation_id = :conversationId
    ) AS sub
    WHERE id = :messageId
    """,
            nativeQuery = true)
    Integer getMessageIndex(
            @Param("conversationId") UUID conversationId,
            @Param("messageId") UUID messageId
    );
}
