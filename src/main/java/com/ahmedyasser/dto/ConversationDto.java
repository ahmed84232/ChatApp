package com.ahmedyasser.dto;

import com.ahmedyasser.entity.Conversation;
import com.ahmedyasser.entity.ConversationParticipant;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class ConversationDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    private String name;
    private List<UUID> userIds;

    private Boolean isGroupChat;


    public static ConversationDto toDto(Conversation conversation) {
        ConversationDto dto = new ConversationDto();

        dto.id = conversation.getId();
        dto.name = conversation.getName();
        dto.userIds = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUserId)
                .collect(Collectors.toList());
        dto.isGroupChat = conversation.isGroupChat();

        return dto;
    }

}
