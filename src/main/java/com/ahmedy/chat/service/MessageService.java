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
            UserDao userDao,
            ObjectMapper objectMapper,
            ConversationService conversationService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
        this.userDao = userDao;
        this.objectMapper = objectMapper;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageDto saveMessage(MessageDto messageDto) {
        Conversation conversation = conversationDao.
                findById(UUID.fromString(messageDto.getConversationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        User sender = userDao
                .findById(UUID.fromString(messageDto.getSenderId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<User> users = conversation.getParticipants().stream().map(ConversationParticipant::getUser).toList();

        if (!users.contains(sender)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant of conversation");
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageText(messageDto.getMessageText());

        return MessageDto.toDto(messageDao.saveAndFlush(message));
    }

    @Transactional
    @CacheEvict(
            cacheNames = "mainCache",
            key = "#actionDto.object.conversationId + ':' + #root.target.getMessagePage(#actionDto)"
    )
    public MessageDto updateMessage(ActionDto<MessageDto> actionDto) {
        MessageDto messageDto = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );

        messageDao.getMessageIndex(UUID.fromString(messageDto.getConversationId()), messageDto.getId());

        List<String> participantIds = conversationService
                .getConversation(UUID.fromString(messageDto.getConversationId()))
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .filter(id -> !id.equals(messageDto.getSenderId()))
                .toList();

        for (String userId : participantIds) {
            messagingTemplate.convertAndSend("/queue/notification.user." + userId, actionDto);
        }
        return updateMessage(messageDto);
    }

    @Transactional
    public MessageDto updateMessage(MessageDto messageDto) {
        Message message = messageDao.findById(messageDto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        Conversation conversation = conversationDao.findById(
                        UUID.fromString(messageDto.getConversationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        User sender = userDao.findById(UUID.fromString(messageDto.getSenderId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<User> users = conversation.getParticipants().stream().map(ConversationParticipant::getUser).toList();

        if (!users.contains(sender)) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant of conversation");
        }

        if (messageDto.getMessageText() != null) {
            message.setMessageText(messageDto.getMessageText());
        }

        if (messageDto.getStatus() != null) {
            message.setStatus(messageDto.getStatus());
        }

        Message savedMessage = messageDao.saveAndFlush(message);

        return MessageDto.toDto(savedMessage);
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

    @Transactional
    @CacheEvict(
        value = "mainCache",
        key = "#actionDto.object.conversationId + ':' + #root.target.getMessagePage(#actionDto)"
    )
    public void sendMessage(ActionDto<MessageDto> actionDto) {
        MessageDto savedMessage = saveMessage(actionDto.getObject());

        List<String> users = conversationService
                .getConversation(UUID.fromString(savedMessage.getConversationId()))
                .getUsers()
                .stream()
                .map(UserDto::getId)
                .toList();

        actionDto.setObject(savedMessage);

        for (String id : users) {
            if (!id.equals(savedMessage.getSenderId())) {
                messagingTemplate.convertAndSend("/queue/notification.user." + id, actionDto);
            }
        }

        messagingTemplate.convertAndSend("/queue/action.user." + savedMessage.getSenderId(), actionDto);
    }

    public int getMessagePage(ActionDto<MessageDto> actionDto) {
        MessageDto messageToBeUpdated = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );

        Integer messageIndex = messageDao.getMessageIndex(UUID.fromString(messageToBeUpdated.getConversationId()),
                messageToBeUpdated.getId());

        return (int) Math.floor(messageIndex / 20.0);
    }

    @Cacheable(cacheNames = "mainCache", key = "#conversationId + ':' + #page")
    public Page<MessageDto> getMessagesByConversationId(UUID conversationId, Integer page , Integer size) {
        System.out.println(page);
        size = 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").ascending());
        Page<Message> Messages = messageDao.findByConversationId(conversationId, pageable);

        return Messages.map(MessageDto::toDto);
    }

    public int getPageCount(UUID conversationId, Integer size) {
        return messageDao.findByConversationId(conversationId, Pageable.ofSize(size)).getTotalPages();
    }

}
