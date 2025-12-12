package com.ahmedy.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class ActionDto<T> {

    @NotNull
    private String action;

    private Map<String, String> metadata;
    private T object;
}
