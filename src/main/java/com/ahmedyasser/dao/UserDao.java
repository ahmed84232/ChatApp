package com.ahmedyasser.dao;

import com.ahmedyasser.dto.UserDto;
import com.ahmedyasser.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserDao extends JpaRepository<User, UUID> {

    @Query("""
        SELECT new com.ahmedyasser.dto.UserDto(u.id, u.username)
        FROM User u
        WHERE u.id IN :userIds
    """)
    List<UserDto> findUsernamesByIds(@Param("userIds") List<UUID> userIds);
}
