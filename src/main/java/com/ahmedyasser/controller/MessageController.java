package com.ahmedyasser.controller;

import com.ahmedyasser.dto.*;
import com.ahmedyasser.dto.MessageDto;
import com.ahmedyasser.service.MessageService;
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
        @RequestParam(required = false) Integer page
    ) {

        if (page == null)
            page = messageService.getPageCount(conversationId);

        Page<MessageDto> messages = messageService.getMessagesByConversationId(conversationId, page);

        System.out.printf(
            "[Message Controller] | Conversation ID: %s | Page: %s | \n%s\n\n",
            conversationId,
            page,
            messages
        );

        return ResponseEntity.ok(messages);
    }


}
