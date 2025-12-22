package com.ahmedyasser.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(
    name = "conversation_participants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"})
)
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    @ToString.Exclude
    private Conversation conversation;

    private UUID userId;

    @CreationTimestamp
    private LocalDateTime joinedAt;

//    @Column(name = "last_read_message_id")
//    private UUID lastReadMessageId;

    private LocalDateTime lastReadTime;

}
