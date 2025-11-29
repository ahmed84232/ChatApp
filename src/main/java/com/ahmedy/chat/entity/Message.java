package com.ahmedy.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    @ToString.Exclude
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    @ToString.Exclude
    private User sender;

    @Column(name = "message_text", nullable = false)
    private String messageText;

    @CreationTimestamp
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "status")
    private String status = "sent";
}
