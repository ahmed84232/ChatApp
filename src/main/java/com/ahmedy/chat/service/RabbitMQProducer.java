package com.ahmedy.chat.service;

import com.ahmedy.chat.config.RabbitMQConfig;
import com.ahmedy.chat.dto.ActionDto;
import com.ahmedy.chat.dto.MessageDto;
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

    public void sendAction(ActionDto<MessageDto> message, UUID userId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "notification.user." + userId, message);
    }

    public void sendAction(List<MessageDto> message, UUID userId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "notification.user." + userId, message);
    }


}