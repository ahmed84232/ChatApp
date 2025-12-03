package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/users")
    public ResponseEntity<UserDto> addUser(@RequestBody UserDto request) {
        return ResponseEntity.ok(userService.addUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody UserDto request) {

        return ResponseEntity.ok(userService.findUserByUsername(request.getUsername()));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserDto> getUser(@PathVariable String username) {
        return ResponseEntity.ok(userService.findUserByUsername(username));
    }

}
