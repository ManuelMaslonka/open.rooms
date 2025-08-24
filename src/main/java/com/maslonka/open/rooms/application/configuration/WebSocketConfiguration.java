package com.maslonka.open.rooms.application.configuration;

import com.maslonka.open.rooms.application.handler.ChatWsHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final ChatWsHandler chatWsHandler;

    public WebSocketConfiguration(ChatWsHandler chatWsHandler) {
        this.chatWsHandler = chatWsHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWsHandler, "/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
