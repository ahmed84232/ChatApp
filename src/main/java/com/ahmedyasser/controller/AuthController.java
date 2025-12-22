package com.ahmedyasser.controller;

import com.ahmedyasser.entity.User;
import com.ahmedyasser.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService, UserService userService1) {
        this.userService = userService1;
    }

    @PostMapping("/token")
    public ResponseEntity<String> getToken(@RequestBody String body) {
        return userService.getToken(body);
    }

    @PostMapping({"/webhook", "/webhook/"})
    public void webhookHandler(@RequestBody Map<String, Object> body) {
        UUID userId = UUID.fromString((String) body.get("userId"));

        if (userService.findDbUserById(userId) == null) {
            String eventType = (String) body.get("type");
            Map<String, Object> details = (Map<String, Object>) body.get("details");

            String firstName = null;
            String lastName = null;
            String email = null;
            String username = null;

            if (eventType.equals("REGISTER")) {
                firstName = (String) details.get("first_name");
                lastName = (String) details.get("last_name");
                email = (String) details.get("email");
                username = (String) details.get("username");

            } else if (eventType.equals("LOGIN"))
                username = (String) details.get("username");

            User user = User.builder()
                    .id(userId)
                    .username(username)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .build();

            userService.saveUser(user);
        }
    }

}
