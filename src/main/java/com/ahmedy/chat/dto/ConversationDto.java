package com.ahmedy.chat.dto;

import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import com.ahmedy.chat.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class ConversationDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<UserDto> users;


    private String name;
    private List<String> participantIds;


    public static ConversationDto toDto(Conversation conversation) {

        ConversationDto dto = new ConversationDto();
        dto.id = conversation.getId();
        dto.users = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .map(UserDto::toDto).toList();
        dto.name = conversation.getName();
        dto.participantIds = conversation.getParticipants().stream()
                .map(ConversationParticipant::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        return dto;
    }

}
