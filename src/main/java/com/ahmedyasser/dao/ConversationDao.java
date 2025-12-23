package com.ahmedyasser.dao;

import com.ahmedyasser.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ConversationDao extends JpaRepository<Conversation, UUID> {

    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Conversation c
        WHERE c.id = :conversationId
          AND (c.isGroupChat = false OR c.owner = :ownerId)
    """)
    Boolean isPrivateConversationOrIsConversationOwner(
        @Param("conversationId") UUID conversationId,
        @Param("ownerId") UUID ownerId
    );
}
