package com.ahmedyasser.service;

import com.ahmedyasser.dao.ConversationDao;
import com.ahmedyasser.dao.ConversationParticipantDao;
import com.ahmedyasser.dao.MessageDao;
import com.ahmedyasser.dto.ConversationDto;
import com.ahmedyasser.dto.UserDto;
import com.ahmedyasser.entity.Conversation;
import com.ahmedyasser.entity.ConversationParticipant;
import com.ahmedyasser.entity.Message;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ahmedyasser.util.AuthUtil.currentUserId;

@Service
public class ConversationService {

    private final ConversationDao conversationDao;
    private final ConversationParticipantDao conversationParticipantDao;
    private final MessageDao messageDao;
    private final UserService userService;

    public ConversationService(
            ConversationDao conversationDao,
            ConversationParticipantDao conversationParticipantDao,
            MessageDao messageDao, UserService userService) {
        this.conversationDao = conversationDao;
        this.conversationParticipantDao = conversationParticipantDao;
        this.messageDao = messageDao;
        this.userService = userService;
    }

    @Transactional
    public ConversationDto createConversation(ConversationDto conversationDto) {
        UUID creatorId = currentUserId();
        UserDto creator = userService.findUserById(creatorId);

        List<UUID> participantIds = new ArrayList<>();
        participantIds.add(creatorId);
        participantIds.addAll(conversationDto.getUserIds());

        boolean isGroupChat = conversationDto.getUserIds().size() > 1;

        // Private chat
        if (!isGroupChat) {
            UUID otherUserId = conversationDto.getUserIds().getFirst();

            boolean exists = conversationParticipantDao
                    .findAllByUserId(creatorId)
                    .stream()
                    .map(ConversationParticipant::getConversation)
                    .filter(c -> c.getParticipants().size() == 2)
                    .anyMatch(c ->
                            c.getParticipants().stream()
                                    .anyMatch(p -> p.getUserId().equals(otherUserId))
                    );

            if (exists) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "This conversation already exists"
                );
            }
        }

        // create Conversation
        Conversation conversation = new Conversation();
        conversation.setGroupChat(isGroupChat);

        if (!isGroupChat) {
            UUID otherUserId = conversationDto.getUserIds().getFirst();
            UserDto otherUser = userService.findUserById(otherUserId);
            conversation.setName("%s, %s".formatted(
                    creator.getUsername(),
                    otherUser.getUsername()
            ));
        } else {
            conversation.setName(conversationDto.getName());
        }

        conversationDao.save(conversation);

        // Add participants
        for (UUID userId : participantIds) {
            ConversationParticipant p = new ConversationParticipant();
            p.setUserId(userId);
            conversation.addParticipant(p);
            conversationParticipantDao.save(p);
        }

        return ConversationDto.toDto(conversation);
    }


    @Transactional
    public ConversationDto getConversationById(UUID conversationId) {
        Conversation conversation = conversationDao.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        return ConversationDto.toDto(conversation);
    }

    @Transactional
    @PreAuthorize("@AuthUtil.isConversationMember(#conversationId)")
    public void addParticipant(UUID conversationId, UUID userId) {
        Conversation conv = conversationDao.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (conversationParticipantDao.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has conversation with id " + conversationId);
        }

        if (!conv.isGroupChat()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is a private conversation");
        }

        ConversationParticipant cp = new ConversationParticipant();
        cp.setConversation(conv);
        cp.setUserId(userId);

        conversationParticipantDao.save(cp);
    }

    public List<ConversationDto> getConversationsByUserId(UUID userId) {

        List<ConversationParticipant> participants;
        try {
            participants = conversationParticipantDao.findAllByUserId(userId);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (participants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "User has no conversations yet!");
        }

        return participants
                .stream().map(ConversationParticipant::getConversation).toList()
                .stream().map(ConversationDto::toDto).toList();
    }

    public UUID getConversationIdByMessageId(UUID messageId) {
        Message message = messageDao.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        return message.getConversation().getId();
    }

    public void deleteConversation(UUID conversationId, UUID userId) {

        if (!conversationParticipantDao.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this conversation");
        }

        conversationDao.deleteById(conversationDao.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")).getId());
    }

    @Transactional
    public void markConversationAsRead(
            UUID conversationId,
            UUID userId
    ) {
        ConversationParticipant participant =
                conversationParticipantDao.findByConversationIdAndUserId(conversationId, userId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User is not a participant"
                        ));

        participant.setLastReadTime(LocalDateTime.now());
        conversationParticipantDao.save(participant);
    }

}
