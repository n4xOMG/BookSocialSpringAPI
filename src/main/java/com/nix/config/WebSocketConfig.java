package com.nix.config;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.nix.models.User;
import com.nix.service.UserService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    JwtValidator jwtValidator;

    @Autowired
    UserService userService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // In-memory broker
        config.setApplicationDestinationPrefixes("/app"); // Prefix for @MessageMapping
        config.setUserDestinationPrefix("/user"); // Prefix for user-specific queues
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("http://localhost:3000", "https://tenshiblog.org/")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptorAdapter());
    }

    @Bean
    public AuthChannelInterceptorAdapter authChannelInterceptorAdapter() {
        return new AuthChannelInterceptorAdapter();
    }

    // --- Auth Channel Interceptor ---

    public class AuthChannelInterceptorAdapter implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = accessor.getFirstNativeHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                User getUser = userService.findUserByJwt(token);
                if (token != null && getUser!=null) {
                    Principal user = new StompPrincipal(getUser.getId().toString());
                    accessor.setUser(user);
                } else {
                    throw new MessagingException("Authentication failed for WebSocket connection.");
                }
            }

            return message;
        }

        public class StompPrincipal implements Principal {
            private String name;

            public StompPrincipal(String name) {
                this.name = name;
            }

            @Override
            public String getName() {
                return name;
            }
        }
    }
}