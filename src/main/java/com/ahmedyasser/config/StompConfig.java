package com.ahmedyasser.config;

import com.ahmedyasser.filter.StompInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private final StompInterceptor stompInterceptor;

    public StompConfig(StompInterceptor stompInterceptor) {
        this.stompInterceptor = stompInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enables a simple in-memory broker
        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompInterceptor);
    }

}