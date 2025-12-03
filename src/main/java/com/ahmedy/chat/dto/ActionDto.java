package com.ahmedy.chat.dto;

import com.ahmedy.chat.enums.MessageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class ActionDto {

    private String senderName;
    private UUID senderId;
    private UUID messageID;
    private UUID conversationID;
    @NotNull
    private String action;
    private MessageStatus messageStatus;
    private String messageText;

}
