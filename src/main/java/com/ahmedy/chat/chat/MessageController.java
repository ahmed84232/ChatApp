package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    public MessageController(SimpMessagingTemplate messagingTemplate,
                             MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(MessageDto messageRequest) {

        MessageDto response = messageService.saveMessage(messageRequest.getId(), messageRequest);

        // Broadcast to /topic/conversation/{id}
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + messageRequest.getConversationId(),
                response
        );
    }
}