package com.nix.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

public class YjsHandshakeInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {

		// Extract room ID from URI path
		// Example URL: /yjsws/authapp-chapter-123
		String path = request.getURI().getPath();
		String[] parts = path.split("/");

		if (parts.length > 2) {
			String roomId = parts[2]; // Extract roomId from path
			attributes.put("roomId", roomId);
			return true;
		}

		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
		// Nothing to do here
	}
}