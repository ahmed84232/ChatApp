package com.ahmedyasser.service;

import com.ahmedyasser.dao.ConversationDao;
import com.ahmedyasser.dao.MessageDao;
import com.ahmedyasser.dto.ActionDto;
import com.ahmedyasser.dto.MessageDto;
import com.ahmedyasser.dto.UserDto;
import com.ahmedyasser.entity.Conversation;
import com.ahmedyasser.entity.ConversationParticipant;
import com.ahmedyasser.entity.Message;
import com.ahmedyasser.enums.MessageStatus;
import com.ahmedyasser.util.AuthUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final UserService userService;
    private final RabbitMQProducer rabbitMQProducer;


    public MessageService(
            MessageDao messageDao,
            ConversationDao conversationDao,
            ObjectMapper objectMapper,
            ConversationService conversationService,
            UserService userService, RabbitMQProducer rabbitMQProducer) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
        this.objectMapper = objectMapper;
        this.conversationService = conversationService;
        this.userService = userService;
        this.rabbitMQProducer = rabbitMQProducer;
    }

    @Transactional
    @CacheEvict(
        value = "mainCache",
        key = "#actionDto.object.conversationId + ':' + #root.target.getMessagePage(#actionDto.getObject())"
    )
    @PreAuthorize("@AuthUtil.isConversationMember(#actionDto.getObject().getConversationId())")
    public void sendMessageRealtime(ActionDto<MessageDto> actionDto) {
        MessageDto messageToBeSaved = actionDto.getObject();

        UserDto user = AuthUtil.currentUser();
        messageToBeSaved.setSenderId(user.getId());
        messageToBeSaved.setSenderName(user.getUsername());

        MessageDto savedMessage = saveMessage(messageToBeSaved);

        List<UUID> users = conversationDao
                .findById(savedMessage.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"no Conversation found"))
                .getParticipants()
                .stream()
                .map(ConversationParticipant::getUserId)
                .toList();

        actionDto.setObject(savedMessage);

        for (UUID id : users) {
            rabbitMQProducer.sendAction(actionDto, id);
        }
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
        key = "#actionDto.object.conversationId + ':' + #root.target.getMessagePage(#actionDto.getObject())"
    )
    @PreAuthorize("@AuthUtil.isMessageOwnerAndConversationMember(#actionDto.getObject().getId(), #actionDto.getObject().getConversationId())")
    public MessageDto updateMessageRealtime(ActionDto<MessageDto> actionDto) {
        MessageDto messageDto = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );

        List<UUID> participantIds = conversationService
                .getConversationById(messageDto.getConversationId())
                .getUserIds()
                .stream()
                .filter(id -> !id.equals(messageDto.getSenderId()))
                .toList();

        for (UUID userId : participantIds) {
            rabbitMQProducer.sendAction(actionDto,userId);
        }

        return updateMessageDb(messageDto);
    }

    @Transactional
    public MessageDto updateMessageDb(MessageDto messageDto) {
        Message message = messageDao
                .findById(messageDto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (messageDto.getMessageText() != null) {
            message.setMessageText(messageDto.getMessageText());
        }

        if (messageDto.getStatus() != null) {
            message.setStatus(messageDto.getStatus());
        }

        Message savedMessage = messageDao.saveAndFlush(message);
        return MessageDto.toDto(savedMessage);
    }

    @CacheEvict(
        cacheNames = "mainCache",
        key = "#actionDto.object.conversationId + ':' + #root.target.getMessagePage(#actionDto.getObject())",
        beforeInvocation = true
    )
    @PreAuthorize("@AuthUtil.isMessageOwner(#actionDto.getObject().getId())")
    public void deleteMessageRealTime(ActionDto<MessageDto> actionDto) {

        MessageDto message = objectMapper.convertValue(
                actionDto.getObject(),
                new TypeReference<>() {}
        );

        UUID conversationId =  conversationService.getConversationIdByMessageId(message.getId());

        deleteMessageDb(message.getId());

        List<UUID> users = conversationService
                .getConversationById(conversationId)
                .getUserIds();

        for (UUID id : users) {
            rabbitMQProducer.sendAction(actionDto, id);
        }
    }

    @Transactional
    public void deleteMessageDb(UUID messageId) {
        messageDao.deleteById(messageId);
    }
    
    @Transactional
    public List<MessageDto> bulkUpdateMessageStatus(List<UUID> messageIds, MessageStatus status) {
        messageDao.updateStatusByIds(messageIds, status);
        return messageDao.findOldestAndNewest(messageIds).stream().map(MessageDto::toDto).toList();
    }

    @Cacheable(cacheNames = "mainCache", key = "#conversationId + ':' + #page")
    public Page<MessageDto> getMessagesByConversationId(UUID conversationId, Integer page) {
        int size = 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").ascending());
        Page<Message> Messages = messageDao.findByConversationId(conversationId, pageable);

        return Messages.map(MessageDto::toDto);
    }
    
    public int getMessagePage(MessageDto messageDto) {
        MessageDto messageToBeUpdated = objectMapper.convertValue(
            messageDto,
            new TypeReference<>() {}
        );

        Integer messageIndex = messageDao.getMessageIndex(
            messageToBeUpdated.getConversationId(),
            messageToBeUpdated.getId()
        );

        System.out.printf(
            "[Message Service] | Message Index: %s | Message Page: %f\n",
            messageIndex,
            Math.floor(messageIndex / 20.0)
        );

        return (int) Math.floor(messageIndex / 20.0);
    }

    public Integer getPageCount(UUID conversationId) {
        return messageDao.findByConversationId(conversationId, Pageable.ofSize(20)).getTotalPages();
    }
    
    public Message getMessage(UUID messageId) {
        return messageDao.findById(messageId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }
}
