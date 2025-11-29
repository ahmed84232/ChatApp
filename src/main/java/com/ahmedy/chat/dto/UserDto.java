package com.ahmedy.chat.dto;

import com.ahmedy.chat.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserDto {

    private String username;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

public static UserDto toDto(User user) {

    UserDto userDto = new UserDto();
    userDto.setUsername(user.getUsername());
    userDto.setId(user.getId().toString());
    userDto.setCreatedAt(user.getCreatedAt());
    return userDto;

}
}
