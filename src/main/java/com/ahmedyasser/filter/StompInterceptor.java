package com.ahmedyasser.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

@Component
public class StompInterceptor implements ChannelInterceptor {

    @Autowired
    private final JwtDecoder jwtDecoder;

    @Autowired
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public StompInterceptor(JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                Jwt jwt = jwtDecoder.decode(token);
                Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                accessor.setUser(authentication);
            }
        }
        return message;
    }

}
