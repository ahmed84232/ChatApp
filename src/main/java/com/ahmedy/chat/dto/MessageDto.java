package com.ahmedy.chat.dto;

import com.ahmedy.chat.entity.Message;
import com.ahmedy.chat.enums.MessageStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MessageDto implements Serializable {
    private UUID id;
    private String senderName;

    private String conversationId;
    private String senderId;
    private String messageText;

    private MessageStatus status;
    private LocalDateTime sentAt;

    public static MessageDto toDto(Message message) {

        MessageDto response = new MessageDto();
        response.setId(message.getId());
        response.setMessageText(message.getMessageText());
        response.setSentAt(message.getSentAt());
        response.setConversationId(message.getConversation().getId().toString());
        response.setSenderId(message.getSender().getId().toString());
        response.setSenderName(message.getSender().getUsername());
        response.setStatus(message.getStatus());
        return response;
    }

}
