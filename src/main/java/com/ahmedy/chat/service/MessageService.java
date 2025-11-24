package com.ahmedy.chat.service;

import com.ahmedy.chat.dao.ConversationDao;
import com.ahmedy.chat.dao.ConversationParticipantDao;
import com.ahmedy.chat.dao.MessageDao;
import com.ahmedy.chat.dao.UserDao;
import com.ahmedy.chat.dto.MessageDto;
import com.ahmedy.chat.entity.Conversation;
import com.ahmedy.chat.entity.ConversationParticipant;
import com.ahmedy.chat.entity.Message;
import com.ahmedy.chat.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageDao messageDao;
    private final ConversationDao conversationDao;
    private final UserDao userDao;
    private final ConversationParticipantDao conversationParticipantDao;

    public MessageService(
            MessageDao messageDao,
            ConversationDao conversationDao,
            UserDao userDao, ConversationParticipantDao conversationParticipantDao) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
        this.userDao = userDao;
        this.conversationParticipantDao = conversationParticipantDao;
    }

    @Transactional
    public MessageDto saveMessage(UUID senderId, MessageDto req) {

        Conversation conversation = conversationDao.findById(
                        UUID.fromString(req.getConversationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        User sender = userDao.findById(senderId)
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

        MessageDto response = new MessageDto();
        response.setId(savedMessage.getId());
        response.setMessageText(req.getMessageText());
        response.setConversationId(req.getConversationId());
        response.setSenderId(senderId.toString());
        response.setSentAt(savedMessage.getSentAt());

        return response;

    }

    public List<MessageDto> getMessages(UUID conversationId) {
        List<Message> Messages = messageDao.findByConversationIdOrderBySentAtAsc(conversationId);

        return Messages.stream().map(message ->
        {
            MessageDto response = new MessageDto();
            response.setId(message.getId());
            response.setMessageText(message.getMessageText());
            response.setSentAt(message.getSentAt());
            response.setConversationId(message.getConversation().getId().toString());
            response.setSenderId(message.getSender().getUsername());
            return response;
        }
        ).toList();
    }
}
