package com.ahmedyasser.dto;

import com.ahmedyasser.entity.Message;
import com.ahmedyasser.enums.MessageStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class MessageDto implements Serializable {
    private UUID id;
    private String senderName;

    private UUID senderId;
    private UUID conversationId;
    private String messageText;

    private MessageStatus status;
    private String sentAt;

    public static MessageDto toDto(Message message) {
        MessageDto messageDto = new MessageDto();

        messageDto.setId(message.getId());
        messageDto.setMessageText(message.getMessageText());
        messageDto.setSentAt(message.getSentAt().toString());
        messageDto.setConversationId(message.getConversation().getId());
        messageDto.setSenderName(message.getUsername());
        messageDto.setSenderId(message.getSenderId());
        messageDto.setStatus(message.getStatus());

        return messageDto;
    }

}
