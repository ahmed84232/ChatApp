package com.ahmedy.chat.chat;

import com.ahmedy.chat.dao.ConversationDao;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.service.ConversationService;
import com.ahmedy.chat.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ConversationService conversationService;

    public MessageController(SimpMessagingTemplate messagingTemplate,
                             MessageService messageService, ConversationDao conversationDao, ConversationService conversationService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.conversationService = conversationService;
    }

    @MessageMapping("/sendMessage/{conversationId}")
    public void sendMessage(MessageDto messageRequestDto, @DestinationVariable String conversationId) {
        messageRequestDto.setConversationId(conversationId);
        MessageDto response = messageService.saveMessage(messageRequestDto);

        List<UserDto> users = conversationService
                .getConversation(UUID.fromString(conversationId))
                .getUsers();

        String recipientId = users.stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(messageRequestDto.getSenderId()))
                .findFirst()
                .orElse(null);

        // Broadcast to /topic/user/{id}
        messagingTemplate.convertAndSend("/topic/user/" + recipientId, response);
        messagingTemplate.convertAndSend("/topic/user/" + messageRequestDto.getSenderId(), response);

        // Assume DM chat
        // User 1 send a message to conversation x
        // backend will keep the sender id on stand by
        // Backend fetches the conversation from DB and get the ids of the users in it
        // The user id that's not the sender id (the receiver id) will be kept on stand by
        // the backend will send the message to topic/user/{receiverId}
        // Receiver will be listening to this topic in frontend already "topic/user/{receiverId}"
    }
}