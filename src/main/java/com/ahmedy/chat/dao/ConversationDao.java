package com.ahmedy.chat.dao;

import com.ahmedy.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationDao extends JpaRepository<Conversation, UUID> {

}
