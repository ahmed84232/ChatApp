package com.ahmedy.chat.dto;

//import com.ahmedy.chat.entity.User;
import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String username;

//
//    public static UserDto toDto(User user) {
//        UserDto dto = new UserDto();
//
//        dto.setId(user.getId());
//        dto.setUsername(user.getName());
//
//        return dto;
//    }
}
