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

    public MessageController(SimpMessagingTemplate messagingTemplate,
                             MessageService messageService, ConversationService conversationService, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    private void sendMessage(MessageDto messageRequestDto) {
        MessageDto response = messageService.saveMessage(messageRequestDto);

        List<String> users = conversationService
                .getConversation(UUID.fromString(messageRequestDto.getConversationId()))
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .toList();

        for (String id : users) {
            if (!id.equals(messageRequestDto.getSenderId())) {
                messagingTemplate.convertAndSend("/queue/notification.user." + id, response);
            }
        }
        messagingTemplate.convertAndSend("/queue/action.user." + messageRequestDto.getSenderId(), response);
    }

    private void deleteMessage(ActionDto actionDto) {

        MessageDto message = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );

        UUID conversationId =  conversationService.getConversationIdByMessageId(message.getId());

        messageService.deleteMessage(message.getId(), UUID.fromString(message.getSenderId()));

        List<String> participants = conversationService
                .getConversation(conversationId)
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(message.getSenderId()))
                .toList();

        for (String id : participants) {

            messagingTemplate.convertAndSend("/queue/notification.user." + id, actionDto);
        }

        messagingTemplate.convertAndSend("/queue/action.user." + message.getSenderId(), actionDto);
    }

    private void deleteConversation(UUID conversationId, UUID senderId) {

        List<String> participants = conversationService
                .getConversation(conversationId)
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .toList();

        conversationService.deleteConversation(conversationId, senderId);

        ActionDto payload = new ActionDto();
        payload.setAction("deleteConversation");
        payload.setConversationID(conversationId);


        for (String id : participants) {
            if (!id.equals(senderId.toString())) {
                messagingTemplate.convertAndSend("/queue/notification.user." + id, payload);
            }
        }
        messagingTemplate.convertAndSend("/queue/action.user." + senderId, conversationId);
    }

    private void typingIndicator(UUID conversationId, UUID senderId) {

        List<String> participants = conversationService
                .getConversation(conversationId)
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(senderId.toString()))
                .toList();

        ActionDto payload = new ActionDto();
        payload.setAction("typingIndicator");
        payload.setConversationID(conversationId);
        payload.setSenderId(senderId);

        for (String id : participants) {
            messagingTemplate.convertAndSend("/queue/notification.user." + id, payload);
        }
    }

    public void messageDelivery(List<MessageDto> messages, UUID senderId, MessageStatus status) {

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

    private void updateMessage(ActionDto actionDto) {

        MessageDto message = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );
        messageService.updateMessage(message);
        List<String> participantIds = conversationService
                .getConversation(UUID.fromString(message.getConversationId()))
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(message.getSenderId()))
                .toList();

        for (String userId : participantIds) {
            messagingTemplate.convertAndSend("/queue/notification.user." + userId, actionDto);
        }
    }


    @MessageMapping("/action")
    public void action(ActionDto actionDto) {
        System.out.println(actionDto.toString());
        if (actionDto.getAction().contains("deleteMessage")) {

            deleteMessage(actionDto);

        } else if (actionDto.getAction().contains("sendMessage")) {

            MessageDto messageDto = new MessageDto();
            messageDto.setSenderId(actionDto.getSenderId().toString());
            messageDto.setMessageText(actionDto.getMessageText());
            messageDto.setConversationId(actionDto.getConversationID().toString());
            sendMessage(messageDto);

        } else if (actionDto.getAction().contains("typingIndicator")) {

            typingIndicator(actionDto.getConversationID(), actionDto.getSenderId());

        } else if (actionDto.getAction().contains("MessageStatus")) {

            MessageStatus ms = MessageStatus.valueOf(String.valueOf(actionDto.getMessageStatus()));

            List<MessageDto> messages = objectMapper.convertValue(
                    actionDto.getObject(),
                    new TypeReference<>() {}
            );
            messageDelivery(messages, actionDto.getSenderId(), ms);

        } else if (actionDto.getAction().contains("deleteConversation")) {

            deleteConversation(actionDto.getConversationID(), actionDto.getSenderId());
        } else if (actionDto.getAction().contains("updateMessage")) {
            updateMessage(actionDto);
        } else throw new IllegalArgumentException("Invalid action: " + actionDto.getAction());

    }
}