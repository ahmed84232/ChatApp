package com.ahmedy.chat.service;

import com.ahmedy.chat.dao.UserDao;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional
    public UserDto findUserById(UUID id) {
        User user = userDao.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserDto response = new UserDto();
        response.setId(user.getId().toString());
        response.setUsername(user.getUsername());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }

    @Transactional
    public UserDto findUserByUsername(String username) {

        User user = userDao.findUserByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserDto response = new UserDto();
        response.setId(user.getId().toString());
        response.setUsername(user.getUsername());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }

    @Transactional
    public UserDto addUser(UserDto req) {

        Optional<User> exUser = userDao.findUserByUsername(req.getUsername());
        if (exUser.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        User savedUser = userDao.saveAndFlush(user);

        UserDto response = new UserDto();

        response.setId(savedUser.getId().toString());
        response.setUsername(savedUser.getUsername());
        response.setCreatedAt(savedUser.getCreatedAt());

        return response;
    }
}
