package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.ConversationDto;
import com.ahmedy.chat.dto.ParticipantRequest;
import com.ahmedy.chat.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.getConversation(conversationId));
    }

    @GetMapping("/conversations/user/{userid}")
    public ResponseEntity<List<ConversationDto>> getConversations(@PathVariable UUID userid) {
        return ResponseEntity.ok(conversationService.getConversations(userid));

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

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable UUID conversationId, @RequestParam UUID participantId) {
        conversationService.deleteConversation(conversationId , participantId);
        return ResponseEntity.noContent().build();
    }
}
