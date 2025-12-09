package com.ahmedy.chat.controller;

import com.ahmedy.chat.dto.ActionDto;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.enums.MessageStatus;
import com.ahmedy.chat.service.ConversationService;
import com.ahmedy.chat.service.MessageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/message")
public class RealtimeController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public RealtimeController(
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
        MessageDto message = actionDto.getObject();

        List<UUID> participants = conversationService
                .getConversation(message.getConversationId())
                .getUserIds();

        conversationService.deleteConversation(message.getConversationId(), message.getSenderId());

        for (UUID id : participants) {
            if (!id.equals(message.getSenderId())) {
                messagingTemplate.convertAndSend("/queue/notification.user." + id, actionDto);
            }
        }
        messagingTemplate.convertAndSend(
                "/queue/action.user." + message.getSenderId(),
                message.getConversationId());
    }

    private void typingIndicator(ActionDto<MessageDto> actionDto) {

        List<UUID> participants = conversationService
                .getConversation(UUID.fromString(String.valueOf(actionDto.getMetadata().get("conversationId"))))
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(UUID.fromString(actionDto.getMetadata().get("senderId"))))
                .toList();


        for (UUID id : participants) {
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

        List<UUID> participantIds = conversationService
                .getConversation(messages.getFirst().getConversationId())
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(senderId))
                .toList();

        for (UUID userId : participantIds) {
            messagingTemplate.convertAndSend("/queue/notification.user." + userId, messages);
        }
    }

}