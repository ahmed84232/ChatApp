package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.*;
import com.ahmedy.chat.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final MessageService messageService;

    public ChatController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageDto>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Page<MessageDto> messages = messageService.getMessagesByConversationId(conversationId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/{conversationId}/pages")
    public int getPages(@PathVariable UUID conversationId , @RequestParam Integer size) {
        return messageService.getPageCount(conversationId, size);
    }

    @PatchMapping("/messages")
    public ResponseEntity<MessageDto> updateMessage(@RequestBody MessageDto request) {
        return ResponseEntity.ok(messageService.updateMessage(request));

    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId, @RequestParam UUID senderId) {
        messageService.deleteMessage(messageId, senderId);
        return ResponseEntity.noContent().build();
    }

}
