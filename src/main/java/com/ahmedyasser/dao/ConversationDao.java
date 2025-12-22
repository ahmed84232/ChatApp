package com.ahmedyasser.dao;

import com.ahmedyasser.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationDao extends JpaRepository<Conversation, UUID> {

}
