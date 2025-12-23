package com.ahmedyasser.service;

import com.ahmedyasser.dao.ConversationDao;
import com.ahmedyasser.dao.ConversationParticipantDao;
import com.ahmedyasser.entity.Conversation;
import com.ahmedyasser.entity.ConversationParticipant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ParticipantService {

    private final ConversationDao conversationDao;
    private final ConversationParticipantDao conversationParticipantDao;

    public ParticipantService(ConversationDao conversationDao, ConversationParticipantDao conversationParticipantDao) {
        this.conversationDao = conversationDao;
        this.conversationParticipantDao = conversationParticipantDao;
    }

    @Transactional
    @PreAuthorize("@AuthUtil.isConversationParticipant(#conversationId)")
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

    @PreAuthorize(
        "@AuthUtil.isPrivateConversationOrIsConversationOwner(#conversationId) || " +
        "@AuthUtil.isConversationParticipantAndUserValidated(#conversationId, #userId)"
    )
    @Transactional
    public void deleteParticipant(UUID conversationId, UUID userId) {

        Conversation conversation = conversationDao.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        ConversationParticipant participant = conversationParticipantDao
            .findByConversationIdAndUserId(conversationId, userId)
            .orElseThrow(
                    () -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "User is not a participant of this conversation " + conversationId
                    )
            );

        conversation.getParticipants().remove(participant);
        if (conversation.isGroupChat()) {

            if (conversation.getOwner().equals(userId)) {

                if (conversation.getParticipants().isEmpty()) {
                    conversationDao.delete(conversation);
                    return;
                }

                conversation.setOwner(conversation.getParticipants().getFirst().getUserId());
            }

            conversationDao.save(conversation);

        } else {

            // Private chat delete the whole problem
            conversationDao.delete(conversation);

        }

    }

}
