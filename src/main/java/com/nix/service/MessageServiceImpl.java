package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.dtos.ChatSummaryDTO;
import com.nix.dtos.MessageDTO;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Message;
import com.nix.models.User;
import com.nix.repository.MessageRepository;
import com.nix.repository.UserRepository;

@Service
public class MessageServiceImpl implements MessageService {
	@Autowired
	MessageRepository messageRepository;

	@Autowired
	UserRepository userRepository;

	@Override
	public Message saveMessage(Integer senderUserid, Integer receiverUserid, MessageDTO message) {
		User sender = userRepository.findById(senderUserid)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + senderUserid));

		User receiver = userRepository.findById(receiverUserid)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + receiverUserid));

		Message newMessage = new Message();
		newMessage.setSender(sender);
		newMessage.setReceiver(receiver);
		newMessage.setContent(message.getContent());
		newMessage.setTimestamp(LocalDateTime.now());
		newMessage.setRead(message.isRead());
		return messageRepository.save(newMessage);
	}

	@Override
	public List<Message> getMessageHistory(Integer senderUserid, Integer receiverUserid) {
		User sender = userRepository.findById(senderUserid)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + senderUserid));

		User receiver = userRepository.findById(receiverUserid)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + receiverUserid));

		return messageRepository.findBySenderAndReceiverOrderByTimestampAsc(sender, receiver);
	}

	@Override
	public List<Message> getUnreadMessages(Integer receiverUserid) {
		User receiver = userRepository.findById(receiverUserid)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + receiverUserid));

		return messageRepository.findByReceiver(receiver);
	}

	@Override
	public void markMessageAsRead(Long messageId) {
		messageRepository.findById(messageId).ifPresent(message -> {
			message.setRead(true);
			messageRepository.save(message);
		});

	}

	@Override
	public List<ChatSummaryDTO> getUserChats(Integer userId) {
		List<Message> messages = messageRepository.findLatestMessagesWithChats(userId);
		return messages.stream().map(m -> new ChatSummaryDTO(
				m.getSender().getId().equals(userId) ? m.getReceiver().getId() : m.getSender().getId(),
				m.getSender().getId().equals(userId) ? m.getReceiver().getFullname() : m.getSender().getFullname(),
				m.getSender().getId().equals(userId) ? m.getReceiver().getAvatarUrl() : m.getSender().getAvatarUrl(),
				m.getContent(), m.getTimestamp())).collect(Collectors.toList());
	}

}
