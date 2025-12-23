package com.ahmedyasser.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue mainQueue(@Value("${pod.name}") String queueName) {
        return new Queue("queue." + queueName, true);
    }

    @Bean
    public FanoutExchange mainExchange() {
        return new FanoutExchange("exchage.main", true, false);
    }

    @Bean
    public Binding mainQueueBinding(Queue queue, FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}