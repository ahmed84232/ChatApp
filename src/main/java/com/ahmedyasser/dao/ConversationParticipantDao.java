package com.ahmedyasser.dao;

import com.ahmedyasser.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationParticipantDao extends JpaRepository<ConversationParticipant, UUID> {

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findAllByUserId(UUID userId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);


}
