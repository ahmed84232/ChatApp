package com.ahmedy.chat.service;

import com.ahmedy.chat.dao.ConversationDao;
import com.ahmedy.chat.dao.ConversationParticipantDao;
import com.ahmedy.chat.dao.UserDao;
import com.ahmedy.chat.dto.ConversationDto;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import com.ahmedy.chat.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationDao conversationDao;
    private final ConversationParticipantDao conversationParticipantDao;
    private final UserDao userDao;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationService(
            ConversationDao conversationDao,
            ConversationParticipantDao conversationParticipantDao,
            UserDao userDao, SimpMessagingTemplate messagingTemplate) {
        this.conversationDao = conversationDao;
        this.conversationParticipantDao = conversationParticipantDao;
        this.userDao = userDao;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ConversationDto createConversation(ConversationDto req, UUID creatorId) {

        User creator = userDao.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<UserDto> users = new ArrayList<>();
        users.add(UserDto.toDto(creator));

        // Handle 1-to-1 conversation check
        if (req.getParticipantIds().size() == 1) {
            UUID otherUserId = UUID.fromString(req.getParticipantIds().getFirst());
            User otherUser = userDao.findById(otherUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            List<Conversation> commonConversations = conversationParticipantDao
                    .findAllByUserId(creatorId).stream()
                    .map(ConversationParticipant::getConversation)
                    .filter(c -> c.getParticipants().size() == 2)
                    .filter(c -> c.getParticipants().stream()
                            .anyMatch(p -> p.getUser().getId().equals(otherUserId)))
                    .toList();

            if (!commonConversations.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This conversation already exists");
            }
        }

        // If no existing conversation found, create new one
        Conversation conv = new Conversation();
        conv.setName(req.getName());
        Conversation savedConversation = conversationDao.save(conv);

        // Add creator
        ConversationParticipant cp = new ConversationParticipant();
        cp.setConversation(savedConversation);
        cp.setUser(creator);
        conversationParticipantDao.saveAndFlush(cp);

        // Add other participants
        for (String id : req.getParticipantIds()) {
            User u = userDao.findById(UUID.fromString(id))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            ConversationParticipant participant = new ConversationParticipant();
            participant.setConversation(savedConversation);
            participant.setUser(u);
            conversationParticipantDao.saveAndFlush(participant);

            users.add(UserDto.toDto(u));
        }

        ConversationDto response = ConversationDto.toDto(savedConversation);
        response.setUsers(users);

        // Broadcast to participants
        response.getUsers().forEach(user ->
                messagingTemplate.convertAndSend("/topic/conversations/" + user.getId(), response)
        );

        return response;
    }



    @Transactional
    public ConversationDto getConversation(UUID conversationId) {
        Conversation conversation = conversationDao.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        return ConversationDto.toDto(conversation);
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

    public List<ConversationDto> getConversations(UUID userId) {

        List<ConversationParticipant> participants = conversationParticipantDao.findAllByUserId(userId);

        if (participants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return participants
                .stream().map(ConversationParticipant::getConversation).toList()
                .stream().map(ConversationDto::toDto).toList();
    }
}
