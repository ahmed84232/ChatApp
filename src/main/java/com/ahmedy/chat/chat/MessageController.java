package com.ahmedy.chat.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {

    @MessageMapping("/message") // client sends to /app/message
    @SendTo("/topic/messages")  // response is broadcast to /topic/messages
    public String handleMessage(String message) {
        return "Echo: " + message;
    }




}