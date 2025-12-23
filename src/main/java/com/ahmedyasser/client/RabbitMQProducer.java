package com.ahmedyasser.client;

import com.ahmedyasser.dto.ActionDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendAction(ActionDto<?> message, UUID userId) {
        rabbitTemplate.convertAndSend("exchage.main", "notification.user." + userId, message);
    }

}