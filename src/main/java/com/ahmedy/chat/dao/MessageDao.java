package com.ahmedy.chat.dao;

import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.Message;
import com.ahmedy.chat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

public interface MessageDao extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId,
                                                       Pageable pageable);

    List<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId);
}
