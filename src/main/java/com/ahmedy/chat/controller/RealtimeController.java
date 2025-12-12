package com.ahmedy.chat.controller;

import com.ahmedy.chat.dto.ActionDto;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.enums.MessageStatus;
import com.ahmedy.chat.service.ConversationService;
import com.ahmedy.chat.service.MessageService;
import com.ahmedy.chat.service.RabbitMQProducer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/message")
public class RealtimeController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final RabbitMQProducer rabbitProducer;

    public RealtimeController(
            MessageService messageService,
            ConversationService conversationService,
            ObjectMapper objectMapper, RabbitMQProducer rabbitProducer
    ) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.rabbitProducer = rabbitProducer;
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
            rabbitProducer.sendAction(actionDto, id);
        }

    }

    private void typingIndicator(ActionDto<MessageDto> actionDto) {

        List<UUID> participants = conversationService
                .getConversation(UUID.fromString(String.valueOf(actionDto.getMetadata().get("conversationId"))))
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(UUID.fromString(actionDto.getMetadata().get("senderId"))))
                .toList();


        for (UUID id : participants) {
            rabbitProducer.sendAction(actionDto, id);
        }
    }

    private void messageDelivery(ActionDto<List<MessageDto>> actionDto) {

        UUID senderId = UUID.fromString(actionDto.getMetadata().get("senderId"));
        MessageStatus status = MessageStatus.valueOf(actionDto.getMetadata().get("messageStatus"));

        List<MessageDto> messages = actionDto.getObject();

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
            rabbitProducer.sendAction(messages, userId);
        }
    }

}