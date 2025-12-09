package com.ahmedy.chat.controller;

import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/token")
    public ResponseEntity<String> getToken(@RequestBody String body) {
        return userService.getToken(body);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsersByUsername(@RequestParam String query) {
        return ResponseEntity.ok(userService.findUserByUsername(query));
    }

}
