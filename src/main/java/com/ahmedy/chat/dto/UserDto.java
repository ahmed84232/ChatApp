package com.ahmedy.chat.dto;

//import com.ahmedy.chat.entity.User;
import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String username;

}
