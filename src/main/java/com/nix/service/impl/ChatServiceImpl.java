package com.nix.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.MessageDTO;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Chat;
import com.nix.models.Message;
import com.nix.models.User;
import com.nix.repository.ChatRepository;
import com.nix.repository.UserRepository;
import com.nix.service.ChatService;

@Service
public class ChatServiceImpl implements ChatService {

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public Chat createOrGetChat(User userOne, User userTwo) {
		Optional<Chat> chatOpt = chatRepository.findByUserOneAndUserTwo(userOne, userTwo);
		if (chatOpt.isPresent()) {
			return chatOpt.get();
		}
		Chat chat = new Chat();
		chat.setUserOne(userOne);
		chat.setUserTwo(userTwo);
		return chatRepository.save(chat);
	}

	@Override
	public Optional<Chat> findById(UUID id) {
		return chatRepository.findById(id);
	}

	@Override
	public List<Chat> getChatsForUser(User user) {
		return chatRepository.findByUserOneOrUserTwo(user, user);
	}

	@Override
	@Transactional
	public Message saveMessage(Chat chat, MessageDTO message) {
		User sender = userRepository.findById(message.getSender().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Cannot find user with id"));

		User receiver = userRepository.findById(message.getReceiver().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Cannot find user with id"));
		Message newMessage = new Message();
		newMessage.setChat(chat);
		newMessage.setSender(sender);
		newMessage.setReceiver(receiver);
		newMessage.setContent(message.getContent());
		if (message.getImage() != null) {
			newMessage.setImage(
					new com.nix.models.ImageAttachment(message.getImage().getUrl(), message.getImage().getIsMild()));
		}
		;
		newMessage.setRead(false);
		newMessage.setTimestamp(LocalDateTime.now());
		chat.getMessages().add(newMessage);
		chatRepository.save(chat);

		return newMessage;
	}

	@Override
	@Transactional
	public void deleteChat(UUID chatId, User currentUser) throws java.nio.file.AccessDeniedException {
		Chat chat = chatRepository.findById(chatId)
				.orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

		// Verify the user is a participant
		if (!chat.getUserOne().equals(currentUser) && !chat.getUserTwo().equals(currentUser)) {
			throw new java.nio.file.AccessDeniedException("You are not a participant of this chat");
		}

		chatRepository.delete(chat);
	}

}
