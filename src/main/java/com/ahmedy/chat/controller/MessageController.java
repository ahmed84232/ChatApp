package com.ahmedy.chat.controller;

import com.ahmedy.chat.dto.*;
import com.ahmedy.chat.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<MessageDto>> getMessagesByConversationId(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") Integer page
    ) {
        Page<MessageDto> messages = messageService.getMessagesByConversationId(conversationId, page);
        return ResponseEntity.ok(messages);
    }


}
