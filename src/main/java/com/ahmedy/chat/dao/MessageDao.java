package com.ahmedy.chat.dao;

import com.ahmedy.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageDao extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId);

}
