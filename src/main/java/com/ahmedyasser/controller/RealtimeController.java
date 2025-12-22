package com.ahmedyasser.controller;

import com.ahmedyasser.dto.ActionDto;
import com.ahmedyasser.dto.MessageDto;
import com.ahmedyasser.enums.MessageStatus;
import com.ahmedyasser.service.ConversationService;
import com.ahmedyasser.service.MessageService;
import com.ahmedyasser.service.RabbitMQProducer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/message")
public class RealtimeController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final RabbitMQProducer rabbitProducer;
    private final CacheManager cacheManager;

    public RealtimeController(
            MessageService messageService,
            ConversationService conversationService,
            ObjectMapper objectMapper, RabbitMQProducer rabbitProducer, CacheManager cacheManager
    ) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.rabbitProducer = rabbitProducer;
        this.cacheManager = cacheManager;
    }

    @MessageMapping("/action")
    public void action(ActionDto<?> actionDto, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (actionDto.getAction().contains("sendMessage")) {
            System.out.printf("\n[Realtime Controller] | Send Message Action:- \n%s\n\n",  actionDto);
            messageService.sendMessageRealtime(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("updateMessage")) {
            messageService.updateMessageRealtime(objectMapper.convertValue(actionDto, new TypeReference<ActionDto<MessageDto>>() {}));

        } else if (actionDto.getAction().contains("deleteMessage")) {
            messageService.deleteMessageRealTime(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("typingIndicator")) {
            typingIndicator(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("MessageStatus")) {
            System.out.printf("\n[Realtime Controller] | Message Status Action:- \n%s\n\n",  actionDto);
            messageDelivery(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else if (actionDto.getAction().contains("deleteConversation")) {
            deleteConversation(objectMapper.convertValue(actionDto, new TypeReference<>() {}));

        } else throw new IllegalArgumentException("Invalid action: " + actionDto.getAction());

    }

    private void deleteConversation(ActionDto<MessageDto> actionDto) {
        MessageDto message = actionDto.getObject();

        List<UUID> participants = conversationService
                .getConversationById(message.getConversationId())
                .getUserIds();

        conversationService.deleteConversation(message.getConversationId(), message.getSenderId());

        for (UUID id : participants) {
            rabbitProducer.sendAction(actionDto, id);
        }

    }

    private void typingIndicator(ActionDto<MessageDto> actionDto) {

        List<UUID> participants = conversationService
                .getConversationById(UUID.fromString(String.valueOf(actionDto.getMetadata().get("conversationId"))))
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(UUID.fromString(actionDto.getMetadata().get("senderId"))))
                .toList();


        for (UUID id : participants) {
            rabbitProducer.sendAction(actionDto, id);
        }
    }

    private void messageDelivery(ActionDto<List<UUID>> actionDto) {
        UUID senderId = UUID.fromString(actionDto.getMetadata().get("senderId"));
        MessageStatus status = MessageStatus.valueOf(actionDto.getMetadata().get("messageStatus"));

        List<UUID> messageIds = actionDto.getObject();
        List<MessageDto> oldestNewestMessages = messageService.bulkUpdateMessageStatus(messageIds, status);

        UUID conversationId = oldestNewestMessages.getFirst().getConversationId();
//        conversationService.markConversationAsRead(conversationId, senderId);

        List<Integer> pages = new ArrayList<>(List.of(
                messageService.getMessagePage(oldestNewestMessages.getFirst()),
                messageService.getMessagePage(oldestNewestMessages.getLast())
        ));

        Cache cache = cacheManager.getCache("mainCache");
        if (cache != null) {
            for (int i = pages.getFirst(); i <= pages.getLast(); i++) {
                cache.evict(conversationId + ":" + i);
            }
        }

        List<UUID> participantIds = conversationService
                .getConversationById(conversationId)
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(senderId))
                .toList();

        for (UUID userId : participantIds) {
            rabbitProducer.sendAction(actionDto, userId);
        }
    }

}