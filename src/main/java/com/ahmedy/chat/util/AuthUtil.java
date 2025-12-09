package com.ahmedy.chat.util;

import com.ahmedy.chat.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@Component
public class AuthUtil {

    private final MessageService messageService;

    public AuthUtil(MessageService messageService) {
        this.messageService = messageService;
    }

    public static UUID currentUserId() {

        if (Objects.equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authorization required");
        }
        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = (String) principal.getClaims().get("sub");
        return UUID.fromString(userId);
    }

    public Boolean isMessageOwner(UUID messageId){
        if (Objects.equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authorization required");
        }
        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = (String) principal.getClaims().get("sub");


        return messageService.getMessage(messageId).getSenderId().equals(UUID.fromString(userId));
    }
}
