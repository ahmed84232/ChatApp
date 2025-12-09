package com.ahmedy.chat.controller;

import com.ahmedy.chat.dto.ConversationDto;
import com.ahmedy.chat.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(@RequestBody ConversationDto request) {
        ConversationDto created = conversationService.createConversation(request);
        return ResponseEntity.ok(created);
    }

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.getConversation(conversationId));
    }

    @GetMapping("/user/{userid}")
    public ResponseEntity<List<ConversationDto>> getConversations(@PathVariable UUID userid) {
        return ResponseEntity.ok(conversationService.getConversationsByUserId(userid));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable UUID conversationId, @RequestParam UUID userId) {
        conversationService.deleteConversation(conversationId , userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> addParticipant(
            @PathVariable UUID userId,
            @RequestParam UUID conversationId
    ) {
        conversationService.addParticipant(
                conversationId,
                userId
        );
        return ResponseEntity.ok("Participant added");
    }

}
