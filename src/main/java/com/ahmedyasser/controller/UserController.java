package com.ahmedyasser.controller;

import com.ahmedyasser.dto.UserDto;
import com.ahmedyasser.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> getUsersByUsername(@RequestParam String query) {
        return ResponseEntity.ok(userService.findUserByUsername(query));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestBody List<String> userIds) {

        return ResponseEntity.ok(
                userService.findAllUsernamesByUserIds(userIds.stream().map(UUID::fromString).toList())
        );
    }

}
