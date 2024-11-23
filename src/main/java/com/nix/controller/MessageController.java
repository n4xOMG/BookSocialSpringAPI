package com.nix.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.nix.dtos.ChatSummaryDTO;
import com.nix.dtos.MessageDTO;
import com.nix.models.Message;
import com.nix.models.User;
import com.nix.request.ChatHistoryRequest;
import com.nix.response.ChatMessageResponse;
import com.nix.service.MessageService;
import com.nix.service.UserService;

@Controller
public class MessageController {
	@Autowired
	private MessageService messageService;

	@Autowired
	UserService userService;
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@GetMapping("/api/chats")
	public ResponseEntity<List<ChatSummaryDTO>> getUserChats(@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		List<ChatSummaryDTO> chatSummaries = messageService.getUserChats(user.getId());
		return ResponseEntity.ok(chatSummaries);
	}

	@MessageMapping("/api/chat.sendMessage")
	public void sendMessage(@Payload MessageDTO message, @RequestHeader("Authorization") String jwt) {
		User sender = userService.findUserByJwt(jwt);
		Integer receiverId = message.getReceiver().getId();

		// Save message to the database
		Message savedMessage = messageService.saveMessage(sender.getId(), receiverId, message);

		// Forward the message to the receiver's queue
		messagingTemplate.convertAndSendToUser(receiverId.toString(), // User destination uses user ID as string
				"/queue/messages",
				new ChatMessageResponse(savedMessage.getId(), savedMessage.getSender().getId(),
						savedMessage.getReceiver().getId(), savedMessage.getContent(), savedMessage.getTimestamp(),
						savedMessage.isRead()));
	}

	/**
	 * Handles fetching message history between two users.
	 *
	 * @param message Incoming message payload containing sender and receiver.
	 */
	@MessageMapping("/api/chat.getHistory")
	@SendToUser("/queue/history")
	public List<Message> getMessageHistory(@Payload ChatHistoryRequest historyRequest, Principal principal) {
		Integer senderId = Integer.parseInt(principal.getName());
		Integer receiverId = historyRequest.getReceiverId();

		return messageService.getMessageHistory(senderId, receiverId);
	}
}
