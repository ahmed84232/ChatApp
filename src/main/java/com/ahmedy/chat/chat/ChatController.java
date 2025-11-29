package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.*;
import com.ahmedy.chat.entity.User;
import com.ahmedy.chat.service.ConversationService;
import com.ahmedy.chat.service.MessageService;
import com.ahmedy.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserService userService;

    public ChatController(ConversationService conversationService,
                          MessageService messageService, UserService userService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.getConversation(conversationId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
            @PathVariable UUID conversationId
    ) {
        List<MessageDto> messages = messageService.getMessages(conversationId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/user/{userid}")
    public ResponseEntity<List<ConversationDto>> getConversations(@PathVariable UUID userid) {
        return ResponseEntity.ok(conversationService.getConversations(userid));

    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDto> sendMessage(@RequestBody MessageDto request) {
        MessageDto saved = messageService.saveMessage(request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationDto> createConversation(
            @RequestBody ConversationDto request,
            @RequestParam UUID creatorId
    ) {
        ConversationDto created = conversationService.createConversation(request, creatorId);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<?> addParticipant(
            @PathVariable UUID conversationId,
            @RequestBody ParticipantRequest request
    ) {
        conversationService.addParticipant(
                conversationId,
                UUID.fromString(request.getUserId())
        );
        return ResponseEntity.ok("Participant added");
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
