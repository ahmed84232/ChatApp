package com.ahmedyasser.service;

import com.ahmedyasser.dto.ActionDto;
import com.ahmedyasser.dto.MessageDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendAction(Object message, UUID userId) {
        rabbitTemplate.convertAndSend("exchage.main", "notification.user." + userId, message);
    }

}