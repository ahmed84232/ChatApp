package com.ahmedy.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MessageDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    private String conversationId;
    private String senderId;
    private String messageText;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime sentAt;

}
