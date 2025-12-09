package com.ahmedy.chat.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

public class AuthUtil {

    public static UUID currentUserId() {

        if (Objects.equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authorization required");
        }
        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = (String) principal.getClaims().get("sub");
        return UUID.fromString(userId);
    }

}
