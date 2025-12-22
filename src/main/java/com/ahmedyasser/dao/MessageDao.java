package com.ahmedyasser.dao;

import com.ahmedyasser.entity.Message;
import com.ahmedyasser.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageDao extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);

    long countByConversationId(UUID conversationId);

    @Query(value = """
    SELECT rn - 1
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (ORDER BY sent_at ASC) AS rn
        FROM chatapp.messages
        WHERE conversation_id = :conversationId
    ) AS sub
    WHERE id = :messageId
    """,
            nativeQuery = true)
    Integer getMessageIndex(
            @Param("conversationId") UUID conversationId,
            @Param("messageId") UUID messageId
    );

    @Modifying
    @Query("""
        UPDATE Message m
        SET m.status = :status
        WHERE m.id IN :ids
    """)
    int updateStatusByIds(
            @Param("ids") List<UUID> ids,
            @Param("status") MessageStatus status
    );

    @Query("""
    SELECT m
    FROM Message m
    WHERE m.id IN :ids
      AND (m.sentAt = (
              SELECT MIN(m2.sentAt)
              FROM Message m2
              WHERE m2.id IN :ids
          )
       OR m.sentAt = (
              SELECT MAX(m3.sentAt)
              FROM Message m3
              WHERE m3.id IN :ids
          ))
    ORDER BY m.sentAt ASC
""")
    List<Message> findOldestAndNewest(@Param("ids") List<UUID> ids);
}
