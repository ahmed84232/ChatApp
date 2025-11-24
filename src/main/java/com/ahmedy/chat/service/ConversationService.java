package com.ahmedy.chat.service;

import com.ahmedy.chat.dao.ConversationDao;
import com.ahmedy.chat.dao.ConversationParticipantDao;
import com.ahmedy.chat.dao.UserDao;
import com.ahmedy.chat.dto.ConversationDto;
import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import com.ahmedy.chat.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationDao conversationDao;
    private final ConversationParticipantDao conversationParticipantDao;
    private final UserService userService;
    private final UserDao userDao;

    public ConversationService(
            ConversationDao conversationDao,
            ConversationParticipantDao conversationParticipantDao,
            UserService userService,
            UserDao userDao) {
        this.conversationDao = conversationDao;
        this.conversationParticipantDao = conversationParticipantDao;
        this.userService = userService;
        this.userDao = userDao;
    }

    @Transactional
    public ConversationDto createConversation(ConversationDto req, UUID creatorId) {

        User creator = userDao.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Create conversation
        Conversation conv = new Conversation();
        conv.setName(req.getName());
        conversationDao.save(conv);

        // Add creator
        ConversationParticipant cp = new ConversationParticipant();
        cp.setConversation(conv);
        cp.setUser(creator);
        conversationParticipantDao.saveAndFlush(cp);

        // Add other participants
        for (String id : req.getParticipantIds()) {
            User u = userDao.findById(UUID.fromString(id))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            ConversationParticipant part = new ConversationParticipant();
            part.setConversation(conv);
            part.setUser(u);

            conversationParticipantDao.saveAndFlush(part);
        }

        ConversationDto response = new ConversationDto();

        response.setId(conv.getId());
        response.setName(req.getName());
        response.setParticipantIds(req.getParticipantIds());

        Conversation savedConv = conversationDao.findById(conv.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        response.setUsers(savedConv.getParticipants().stream().map(ConversationParticipant::getUser).toList());

        return response;
    }

    @Transactional
    public void addParticipant(UUID conversationId, UUID userId) {
        Conversation conv = conversationDao.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Prevent duplicates
        if (conversationParticipantDao.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has conversation with id " + conversationId);
        }

        ConversationParticipant cp = new ConversationParticipant();
        cp.setConversation(conv);
        cp.setUser(user);

        conversationParticipantDao.save(cp);
    }
}
