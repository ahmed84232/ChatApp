package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.ActionDto;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.enums.MessageStatus;
import com.ahmedy.chat.service.ConversationService;
import com.ahmedy.chat.service.MessageService;
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

    public MessageController(SimpMessagingTemplate messagingTemplate,
                             MessageService messageService, ConversationService conversationService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.conversationService = conversationService;
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

    private void deleteMessage(UUID messageId, UUID senderId) {

        UUID conversationId =  conversationService.getConversationIdByMessageId(messageId);

        messageService.deleteMessage(messageId, senderId);

        List<String> participants = conversationService
                .getConversation(conversationId)
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .toList();

        ActionDto payload = new ActionDto();
        payload.setAction("deleteMessage");
        payload.setMessageID(messageId);

        for (String id : participants) {
            if (!id.equals(senderId.toString())) {
                messagingTemplate.convertAndSend("/queue/notification.user." + id, payload);
            }
        }

        messagingTemplate.convertAndSend("/queue/action.user." + senderId, payload);
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

    private void messageDelivery(UUID conversationId, UUID senderId, Enum<MessageStatus> status) {

        List<MessageDto> messages = messageService.getMessagesByConversationId(conversationId);

        List<MessageDto> messageStatus = messages.stream()
                .filter(m -> m.getStatus() == MessageStatus.SENT)
                .toList();
        
        messageStatus.forEach(m -> m.setStatus(status));

        List<String> participantIds = conversationService
                .getConversation(conversationId)
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(senderId.toString()))
                .toList();

        for (String userId : participantIds) {
            messagingTemplate.convertAndSend("/queue/notification.user." + userId, messageStatus);
        }
    }


    @MessageMapping("/action")
    public void action(ActionDto actionDto) {

        if (actionDto.getAction().contains("deleteMessage")) {

            deleteMessage(actionDto.getMessageID(), actionDto.getSenderId());

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
            messageDelivery(actionDto.getConversationID(), actionDto.getSenderId(), ms);

        } else if (actionDto.getAction().contains("deleteConversation")) {

            deleteConversation(actionDto.getConversationID(), actionDto.getSenderId());

        } else throw new IllegalArgumentException("Invalid action: " + actionDto.getAction());

    }
}