package com.ahmedy.chat.dto;

import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
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


    public static ConversationDto toDto(Conversation conversation) {
        ConversationDto dto = new ConversationDto();

        dto.id = conversation.getId();
        dto.name = conversation.getName();
        dto.userIds = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUserId)
                .collect(Collectors.toList());

        return dto;
    }

}
