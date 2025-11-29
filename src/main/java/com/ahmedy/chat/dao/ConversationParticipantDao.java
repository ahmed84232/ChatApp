package com.ahmedy.chat.dao;

import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationParticipantDao extends JpaRepository<ConversationParticipant, UUID> {

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findAllByUserId(UUID userId);

}
