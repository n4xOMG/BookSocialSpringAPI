package com.nix.config;

import java.security.Principal;

import javax.crypto.SecretKey;

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

import com.nix.service.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private static SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
	@Autowired
	private JwtValidator jwtValidator;

	@Autowired
	private UserService userService;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/queue"); // In-memory broker
		config.setApplicationDestinationPrefixes("/app"); // Prefix for @MessageMapping
		config.setUserDestinationPrefix("/user"); // Prefix for user-specific queues
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws-chat").setAllowedOrigins("http://localhost:3000", "https://tenshiblog.org/") // Adjust
																												// as
																												// needed
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
					token = token.substring(7).trim();
				} else {
					throw new MessagingException("Missing or invalid Authorization header.");
				}

				try {
					Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();

					String email = claims.get("email", String.class);
					System.out.println("Authenticated user: " + email);

					// Set user details in WebSocket session
					Principal user = new StompPrincipal(email);
					accessor.setUser(user);

				} catch (JwtException e) {
					System.err.println("JWT validation error: " + e.getMessage());
					throw new MessagingException("Invalid JWT token.");
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