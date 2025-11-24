package com.ahmedy.chat.dao;

import com.ahmedy.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserDao extends JpaRepository<User, UUID> {
}
