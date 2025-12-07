package com.nix.controller;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

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
	public ChatDTO createChat(@PathVariable("otherUserId") UUID otherUserId,
			@RequestHeader("Authorization") String jwt) {
		User currentUser = userService.findUserByJwt(jwt);
		User otherUser = userService.findUserById(otherUserId);
		logger.info("Creating or getting chat for user {} and {}", currentUser.getUsername(), otherUser.getUsername());
		Chat chat = chatService.createOrGetChat(currentUser, otherUser);
		return chatMapper.mapToDTO(chat);
	}

	@PostMapping("/create-message")
	public MessageDTO createMessage(@RequestBody MessageDTO messageDTO) {
		Chat chat = chatService.findById(messageDTO.getChatId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid chat ID"));
		Message newMessage = chatService.saveMessage(chat, messageDTO);
		return messageMapper.mapToDTO(newMessage);
	}

	// Get all chats for the current user
	@GetMapping
	public List<ChatDTO> getUserChats(@RequestHeader("Authorization") String jwt) {
		User currentUser = userService.findUserByJwt(jwt);
		logger.debug("Getting chats for user: {}", currentUser.getUsername());
		List<Chat> chats = chatService.getChatsForUser(currentUser);
		return chatMapper.mapToDTOs(chats);
	}

	@GetMapping("/{chatId}/messages")
	public List<MessageDTO> getChatMessages(@PathVariable UUID chatId, @RequestHeader("Authorization") String jwt)
			throws AccessDeniedException {
		User currentUser = userService.findUserByJwt(jwt);
		Chat chat = chatService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Chat not found"));
		logger.debug("Getting chat messages for user: {}", currentUser.getUsername());
		// Verify that the current user is a participant of the chat
		if (!chat.getUserOne().equals(currentUser) && !chat.getUserTwo().equals(currentUser)) {
			throw new AccessDeniedException("You are not a participant of this chat");
		}

		return messageMapper.mapToDTOs(chat.getMessages());
	}

	@DeleteMapping("/{chatId}")
	public ResponseEntity<String> deleteChat(@PathVariable UUID chatId,
			@RequestHeader("Authorization") String jwt) throws AccessDeniedException {
		User currentUser = userService.findUserByJwt(jwt);
		logger.info("User {} deleting chat {}", currentUser.getUsername(), chatId);
		chatService.deleteChat(chatId, currentUser);
		return ResponseEntity.ok("Chat deleted successfully");
	}

	@MessageMapping("/chat/{groupId}")
	public MessageDTO sendToUser(@Payload MessageDTO message, @DestinationVariable String groupId) {
		logger.info("Received groupId: {}", groupId);
		logger.info("Received MessageDTO: {}", message.toString());

		try {
			String destination = "/group/" + groupId + "/private";
			logger.debug("Sending message to destination: {}", destination);

			messagingTemplate.convertAndSend(destination, message);

			logger.debug("Message successfully sent to {}", destination);
		} catch (Exception e) {
			logger.error("Error sending message: {}", e.getMessage(), e);
		}

		return message;
	}

}
