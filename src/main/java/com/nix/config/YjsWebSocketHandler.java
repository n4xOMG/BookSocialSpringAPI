package com.nix.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class YjsWebSocketHandler extends TextWebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(YjsWebSocketHandler.class);

	// Store sessions by roomId
	private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {
		try {
			// Extract roomId from session attributes
			String roomId = (String) session.getAttributes().get("roomId");

			if (roomId != null && rooms.containsKey(roomId)) {
				// Forward the message to all clients in the same room except sender
				for (WebSocketSession client : rooms.get(roomId).values()) {
					if (client.isOpen() && !client.getId().equals(session.getId())) {
						client.sendMessage(message);
					}
				}
			}
		} catch (IOException e) {
			logger.error("Error handling message", e);
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		String roomId = (String) session.getAttributes().get("roomId");

		if (roomId != null) {
			// Create room if it doesn't exist
			rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

			// Add session to room
			rooms.get(roomId).put(session.getId(), session);

			logger.info("Client connected to room {}: {}", roomId, session.getId());
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
		String roomId = (String) session.getAttributes().get("roomId");

		if (roomId != null && rooms.containsKey(roomId)) {
			// Remove session from room
			rooms.get(roomId).remove(session.getId());

			// Remove room if empty
			if (rooms.get(roomId).isEmpty()) {
				rooms.remove(roomId);
			}

			logger.info("Client disconnected from room {}: {}", roomId, session.getId());
		}
	}
}
