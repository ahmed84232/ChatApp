package com.ahmedy.chat.service;

import com.ahmedy.chat.dao.ConversationDao;
import com.ahmedy.chat.dao.MessageDao;
import com.ahmedy.chat.dao.UserDao;
import com.ahmedy.chat.dto.ActionDto;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import com.ahmedy.chat.entity.Message;
import com.ahmedy.chat.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageDao messageDao;
    private final ConversationDao conversationDao;
    private final UserDao userDao;
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;


    public MessageService(
            MessageDao messageDao,
            ConversationDao conversationDao,
            UserDao userDao, ObjectMapper objectMapper, ConversationService conversationService, SimpMessagingTemplate messagingTemplate) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
        this.userDao = userDao;
        this.objectMapper = objectMapper;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageDto saveMessage(MessageDto req) {

        Conversation conversation = conversationDao.findById(
                        UUID.fromString(req.getConversationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        User sender = userDao.findById(UUID.fromString(req.getSenderId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<User> users = conversation.getParticipants().stream().map(ConversationParticipant::getUser).toList();

        if (!users.contains(sender)) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant of conversation");
        }

        Message msg = new Message();
        msg.setConversation(conversation);
        msg.setSender(sender);
        msg.setMessageText(req.getMessageText());

        Message savedMessage = messageDao.saveAndFlush(msg);

        return MessageDto.toDto(savedMessage);

    }


    @Transactional
    @CacheEvict(
            value = "mainCache",
            key = "#actionDto.object.conversationId +"
                    + " ':' + #actionDto.metadata.get('page') +"
                    + " ':' + #actionDto.metadata.get('size')"
    )
    public void sendMessage(ActionDto actionDto) {

        MessageDto message = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );

        MessageDto response = saveMessage(message);

        List<String> users = conversationService
                .getConversation(UUID.fromString(message.getConversationId()))
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .toList();

        actionDto.setObject(response);

        for (String id : users) {
            if (!id.equals(message.getSenderId())) {
                messagingTemplate.convertAndSend("/queue/notification.user." + id, actionDto);
            }
        }
        messagingTemplate.convertAndSend("/queue/action.user." + message.getSenderId(), actionDto);
    }

    @Transactional
    public MessageDto updateMessage(MessageDto req) {

        Message message = messageDao.findById(req.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        Conversation conversation = conversationDao.findById(
                        UUID.fromString(req.getConversationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        User sender = userDao.findById(UUID.fromString(req.getSenderId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<User> users = conversation.getParticipants().stream().map(ConversationParticipant::getUser).toList();

        if (!users.contains(sender)) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant of conversation");
        }

        if (req.getMessageText() != null) {
            message.setMessageText(req.getMessageText());
        }

        if (req.getStatus() != null) {
            message.setStatus(req.getStatus());
        }

        Message savedMessage = messageDao.saveAndFlush(message);

        return MessageDto.toDto(savedMessage);
    }

    @Cacheable(cacheNames = "mainCache", key = "#conversationId + ':' + #page + ':' + #size")
    public Page<MessageDto> getMessagesByConversationId(UUID conversationId, Integer page , Integer size) {
        System.out.println(page);

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").ascending());
        Page<Message> Messages = messageDao.findByConversationId(conversationId, pageable);

        return Messages.map(MessageDto::toDto);
    }

    public int getPageCount(UUID conversationId, Integer size) {
        return messageDao.findByConversationId(conversationId, Pageable.ofSize(size)).getTotalPages();
    }

    public List<MessageDto> getMessagesByConversationId(UUID conversationId) {
        return messageDao.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(MessageDto::toDto).toList();
    }

    public void deleteMessage(UUID messageId, UUID senderId) {
        boolean belongsToUser = messageDao.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"))
                .getSender().getId().equals(senderId);

        if (!belongsToUser) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "you are not allowed to delete this message");
        }

        messageDao.deleteById(messageDao.findById(UUID.fromString(messageId.toString()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "There is no Message with this id")).getId());
    }

}
