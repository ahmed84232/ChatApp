package com.ahmedyasser.util;

import com.ahmedyasser.dao.ConversationDao;
import com.ahmedyasser.dto.ConversationDto;
import com.ahmedyasser.dto.UserDto;
import com.ahmedyasser.service.ConversationService;
import com.ahmedyasser.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component("AuthUtil")
public class AuthUtil {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ConversationDao conversationDao;

    public AuthUtil(MessageService messageService, ConversationService conversationService, ConversationDao conversationDao) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.conversationDao = conversationDao;
    }

    public static UUID currentUserId() {

        if (Objects.equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authorization required");
        }

        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = (String) principal.getClaims().get("sub");
        return UUID.fromString(userId);
    }

    public static UserDto currentUser() {

        if (Objects.equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authorization required");
        }

        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UUID userId = UUID.fromString((String) principal.getClaims().get("sub"));
        String userName = (String) principal.getClaims().get("preferred_username");

        return UserDto.builder().id(userId).username(userName).build();
    }

    @Transactional
    public Boolean isConversationParticipant(UUID conversationId) {
        List<UUID> conversationIds = conversationService
                .getConversationsByUserId(currentUserId())
                .stream()
                .map(ConversationDto::getId)
                .toList();

        return conversationIds.contains(conversationId);
    }

    @Transactional
    public Boolean isPrivateConversationOrIsConversationOwner(UUID conversationId) {
        return conversationDao.isPrivateConversationOrIsConversationOwner(conversationId, currentUserId());
    }

    @Transactional
    public Boolean validateUserId(UUID userId) {
        return userId.equals(currentUserId());
    }

    @Transactional
    public Boolean isConversationParticipantAndUserValidated(UUID conversationId, UUID userId) {
        return validateUserId(userId) && isConversationParticipant(conversationId);
    }

    @Transactional
    public Boolean isMessageOwner(UUID messageId){
        return messageService
                .getMessage(messageId)
                .getSenderId()
                .equals(currentUserId());
    }

    @Transactional
    public Boolean isMessageOwnerAndConversationParticipant(UUID messageId, UUID conversationId) {
        return isMessageOwner(messageId) && isConversationParticipant(conversationId);
    }
}
