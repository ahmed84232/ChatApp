package com.ahmedy.chat.service;

import com.ahmedy.chat.dao.ConversationDao;
import com.ahmedy.chat.dao.ConversationParticipantDao;
import com.ahmedy.chat.dao.MessageDao;
import com.ahmedy.chat.dao.UserDao;
import com.ahmedy.chat.dto.ConversationDto;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import com.ahmedy.chat.entity.Message;
import com.ahmedy.chat.entity.User;
import org.springframework.http.HttpStatus;
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
    private final MessageDao messageDao;

    public ConversationService(
            ConversationDao conversationDao,
            ConversationParticipantDao conversationParticipantDao,
            UserDao userDao, MessageDao messageDao) {
        this.conversationDao = conversationDao;
        this.conversationParticipantDao = conversationParticipantDao;
        this.userDao = userDao;
        this.messageDao = messageDao;
    }

    @Transactional
    public ConversationDto createConversation(ConversationDto req, UUID creatorId) {

        User creator = userDao.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<UserDto> users = new ArrayList<>();
        users.add(UserDto.toDto(creator));

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

        Conversation conv = new Conversation();
        conv.setName(req.getName());

        if (req.getParticipantIds().size() <= 2) {
            conv.setGroupChat(true);
        }

        Conversation savedConversation = conversationDao.save(conv);

        ConversationParticipant cp = new ConversationParticipant();
        cp.setConversation(savedConversation);
        cp.setUser(creator);
        conversationParticipantDao.saveAndFlush(cp);

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

        if (conversationParticipantDao.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has conversation with id " + conversationId);
        }

        ConversationParticipant cp = new ConversationParticipant();
        cp.setConversation(conv);
        cp.setUser(user);

        conversationParticipantDao.save(cp);
    }

    public List<ConversationDto> getConversations(UUID userId) {

        List<ConversationParticipant> participants = null;
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
}
