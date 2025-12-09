package com.ahmedy.chat.service;

import com.ahmedy.chat.dao.ConversationDao;
import com.ahmedy.chat.dao.MessageDao;
import com.ahmedy.chat.dto.ActionDto;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import com.ahmedy.chat.entity.Message;
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
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;


    public MessageService(
            MessageDao messageDao,
            ConversationDao conversationDao,
            ObjectMapper objectMapper,
            ConversationService conversationService,
            SimpMessagingTemplate messagingTemplate,
            UserService userService) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
        this.objectMapper = objectMapper;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    @Transactional
    public MessageDto saveMessage(MessageDto messageDto) {
        Conversation conversation = conversationDao.
                findById(messageDto.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        UserDto sender = userService.findUserById(messageDto.getSenderId());

        List<UUID> users = conversation.getParticipants().stream().map(ConversationParticipant::getUserId).toList();

        if (!users.contains(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant of conversation");
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setUsername(messageDto.getSenderName());
        message.setSenderId(sender.getId());
        message.setMessageText(messageDto.getMessageText());

        MessageDto dto = MessageDto.toDto(messageDao.saveAndFlush(message));
        dto.setSenderName(sender.getUsername());

        return dto;
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

        messageDao.getMessageIndex(messageDto.getConversationId(), messageDto.getId());

        List<UUID> participantIds = conversationService
                .getConversation(messageDto.getConversationId())
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(messageDto.getSenderId()))
                .toList();

        for (UUID userId : participantIds) {
            messagingTemplate.convertAndSend("/queue/notification.user." + userId, actionDto);
        }
        return updateMessage(messageDto);
    }

    @Transactional
    public MessageDto updateMessage(MessageDto messageDto) {
        Message message = messageDao.findById(messageDto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        Conversation conversation = conversationDao.findById(messageDto.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        UserDto sender = userService.findUserById(messageDto.getSenderId());

        List<UUID> users = conversation.getParticipants().stream().map(ConversationParticipant::getUserId).toList();

        if (!users.contains(sender.getId())) {

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
                .getSenderId().equals(senderId);

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

        List<UUID> users = conversationDao.findById(savedMessage.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"no Conversation found"))
                .getParticipants().stream().map(ConversationParticipant::getUserId).toList();

        actionDto.setObject(savedMessage);

        for (UUID id : users) {
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

        Integer messageIndex = messageDao.getMessageIndex(messageToBeUpdated.getConversationId(),
                messageToBeUpdated.getId());

        return (int) Math.floor(messageIndex / 20.0);
    }

    @Cacheable(cacheNames = "mainCache", key = "#conversationId + ':' + #page")
    public Page<MessageDto> getMessagesByConversationId(UUID conversationId, Integer page) {
        System.out.println(page);
        int size = 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").ascending());
        Page<Message> Messages = messageDao.findByConversationId(conversationId, pageable);

        return Messages.map(MessageDto::toDto);
    }

    public int getPageCount(UUID conversationId, Integer size) {
        return messageDao.findByConversationId(conversationId, Pageable.ofSize(size)).getTotalPages();
    }

    @CacheEvict(
            cacheNames = "mainCache",
            key = "#actionDto.object.conversationId + ':' + #root.target.getMessagePage(#actionDto)",
            beforeInvocation = true
    )
    public void deleteMessageRealTime(ActionDto<MessageDto> actionDto) {

        MessageDto message = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );

        UUID conversationId =  conversationService.getConversationIdByMessageId(message.getId());

        deleteMessage(message.getId(), message.getSenderId());

        List<UUID> users = conversationService
                .getConversation(conversationId)
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(message.getSenderId()))
                .toList();

        for (UUID id : users) {
            messagingTemplate.convertAndSend("/queue/notification.user." + id, actionDto);
        }

        messagingTemplate.convertAndSend("/queue/action.user." + message.getSenderId(), actionDto);
    }
}
