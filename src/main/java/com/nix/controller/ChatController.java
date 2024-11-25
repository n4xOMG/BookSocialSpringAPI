package com.nix.controller;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.ChatDTO;
import com.nix.dtos.MessageDTO;
import com.nix.dtos.mappers.ChatMapper;
import com.nix.dtos.mappers.MessageMapper;
import com.nix.models.Chat;
import com.nix.models.Message;
import com.nix.models.User;
import com.nix.service.ChatService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	@Autowired
	private ChatService chatService;

	@Autowired
	private UserService userService;

	ChatMapper chatMapper = new ChatMapper();

	MessageMapper messageMapper = new MessageMapper();

	// Create or get existing chat between two users
	@PostMapping("/{otherUserId}")
	public ChatDTO createChat(@PathVariable("otherUserId") Integer otherUserId,
			@RequestHeader("Authorization") String jwt) {
		User currentUser = userService.findUserByJwt(jwt);
		User otherUser = userService.findUserById(otherUserId);
		Chat chat = chatService.createOrGetChat(currentUser, otherUser);
		return chatMapper.mapToDTO(chat);
	}

	// Get all chats for the current user
	@GetMapping
	public List<ChatDTO> getUserChats(@RequestHeader("Authorization") String jwt) {
		User currentUser = userService.findUserByJwt(jwt);
		List<Chat> chats = chatService.getChatsForUser(currentUser);
		return chatMapper.mapToDTOs(chats);
	}

	@GetMapping("/{chatId}/messages")
	public List<MessageDTO> getChatMessages(@PathVariable Long chatId, @RequestHeader("Authorization") String jwt)
			throws AccessDeniedException {
		User currentUser = userService.findUserByJwt(jwt);
		Chat chat = chatService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Chat not found"));

		// Verify that the current user is a participant of the chat
		if (!chat.getUserOne().equals(currentUser) && !chat.getUserTwo().equals(currentUser)) {
			throw new AccessDeniedException("You are not a participant of this chat");
		}

		return messageMapper.mapToDTOs(chat.getMessages());
	}

	@MessageMapping("/chat.sendMessage") // This should match "/app/chat.sendMessage"
	public void sendMessage(@Payload MessageDTO message, Principal principal) {
		// Log the received message for debugging
		Chat chat = chatService.findById(message.getChatId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid chat ID"));
		message.setTimestamp(LocalDateTime.now());
		Message newMessage = chatService.saveMessage(chat, message);
		messagingTemplate.convertAndSendToUser(newMessage.getReceiver().getUsername(), "/queue/chat/" + chat.getId(),
				newMessage);
	}
}
