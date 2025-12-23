package com.ahmedyasser.dto;

import com.ahmedyasser.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String username;

    static public UserDto toDto(User user){
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }
}
