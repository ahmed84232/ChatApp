package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.ActionDto;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.enums.MessageStatus;
import com.ahmedy.chat.service.ConversationService;
import com.ahmedy.chat.service.MessageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public MessageController(
            SimpMessagingTemplate messagingTemplate,
            MessageService messageService,
            ConversationService conversationService,
            ObjectMapper objectMapper
    ) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @MessageMapping("/action")
    public void action(ActionDto<?> actionDto) {
        System.out.println(actionDto);

        if (actionDto.getAction().contains("sendMessage")) {
            messageService.sendMessage(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("updateMessage")) {
            messageService.updateMessage(objectMapper.convertValue(actionDto, new TypeReference<ActionDto<MessageDto>>() {}));

        } else if (actionDto.getAction().contains("deleteMessage")) {
            messageService.deleteMessageRealTime(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("typingIndicator")) {
            typingIndicator(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("MessageStatus")) {

            messageDelivery(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("deleteConversation")) {
            deleteConversation(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else throw new IllegalArgumentException("Invalid action: " + actionDto.getAction());

    }

    private void deleteConversation(ActionDto<MessageDto> actionDto) {

        List<String> participants = conversationService
                .getConversation(actionDto.getConversationID())
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .toList();

        conversationService.deleteConversation(actionDto.getConversationID(), actionDto.getSenderId());

        for (String id : participants) {
            if (!id.equals(actionDto.getSenderId().toString())) {
                messagingTemplate.convertAndSend("/queue/notification.user." + id, actionDto);
            }
        }
        messagingTemplate.convertAndSend(
                "/queue/action.user." + actionDto.getSenderId(),
                actionDto.getConversationID());
    }

    private void typingIndicator(ActionDto<MessageDto> actionDto) {

        List<String> participants = conversationService
                .getConversation(UUID.fromString(String.valueOf(actionDto.getMetadata().get("conversationId"))))
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(actionDto.getMetadata().get("senderId")))
                .toList();


        for (String id : participants) {
            messagingTemplate.convertAndSend("/queue/notification.user." + id, actionDto);
        }
    }

    private void messageDelivery(ActionDto<List<MessageDto>> actionDto) {

        UUID senderId = UUID.fromString(actionDto.getMetadata().get("senderId"));
        MessageStatus status = MessageStatus.valueOf(actionDto.getMetadata().get("messageStatus"));

        List<MessageDto> messages = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {});

        messages.forEach(m -> {
            m.setStatus(status);
            messageService.updateMessage(m);
        });

        List<String> participantIds = conversationService
                .getConversation(UUID.fromString(messages.getFirst().getConversationId()))
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(senderId.toString()))
                .toList();

        for (String userId : participantIds) {
            messagingTemplate.convertAndSend("/queue/notification.user." + userId, messages);
        }
    }

}