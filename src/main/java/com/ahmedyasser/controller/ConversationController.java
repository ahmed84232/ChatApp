package com.ahmedyasser.controller;

import com.ahmedyasser.dto.ConversationDto;
import com.ahmedyasser.service.ConversationService;
import com.ahmedyasser.service.ParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversation")
public class ConversationController {

    private final ConversationService conversationService;
    private final ParticipantService participantService;

    public ConversationController(ConversationService conversationService, ParticipantService participantService) {
        this.conversationService = conversationService;
        this.participantService = participantService;
    }

    @GetMapping("/user/{userid}")
    public ResponseEntity<List<ConversationDto>> getConversations(@PathVariable UUID userid) {
        return ResponseEntity.ok(conversationService.getConversationsByUserId(userid));
    }

    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(@RequestBody ConversationDto request) {
        ConversationDto created = conversationService.createConversation(request);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable UUID conversationId, @RequestParam UUID userId) {
        participantService.deleteParticipant(conversationId, userId);
//        conversationService.deleteConversation(conversationId , userId);
        return ResponseEntity.noContent().build();
    }

//    @GetMapping("/{conversationId}")
//    public ResponseEntity<ConversationDto> getConversation(@PathVariable UUID conversationId) {
//        return ResponseEntity.ok(conversationService.getConversation(conversationId));
//    }

}
