package com.ahmedyasser.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    public RabbitMQConsumer(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "#{mainQueue.name}")
    public void listen(Message<?> message) {
        String routingKey = (String) message.getHeaders().get("amqp_receivedRoutingKey");

        assert routingKey != null;

        String[] parts = routingKey.split("\\.");
        String userId = parts[parts.length - 1];

        System.out.printf("\n[Rabbit Consumer] | Topic: /queue/notification.user.%s | Message:-\n%s\n\n", userId, message.getPayload());
        messagingTemplate.convertAndSend("/queue/notification.user." + userId, message.getPayload());
    }

}