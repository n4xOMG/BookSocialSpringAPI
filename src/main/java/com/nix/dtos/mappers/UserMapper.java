package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.UserDTO;
import com.nix.models.User;

import org.springframework.stereotype.Component;

@Component
public class UserMapper implements Mapper<User, UserDTO> {

	RoleMapper roleMapper = new RoleMapper();

	BookMapper bookMapper = new BookMapper();

	CommentMapper commentMapper = new CommentMapper();

	PostMapper postMapper = new PostMapper();

	@Override
	public UserDTO mapToDTO(User user) {
		UserDTO userDTO = new UserDTO();
		userDTO.setId(user.getId());
		userDTO.setUsername(user.getUsername());
		userDTO.setEmail(user.getEmail());
		userDTO.setBirthdate(user.getBirthdate());
		userDTO.setRole(roleMapper.mapToDTO(user.getRole()));
		int balance = user.getWallet() != null ? user.getWallet().getBalance() : 0;
		userDTO.setCredits(balance);
		userDTO.setAvatarUrl(user.getAvatarUrl());
		userDTO.setFullname(user.getFullname());
		userDTO.setBio(user.getBio());
		userDTO.setGender(user.getGender());
		userDTO.setBanned(user.isBanned());
		userDTO.setBanReason(user.getBanReason());
		userDTO.setIsVerified(user.getIsVerified());
		userDTO.setIsSuspended(user.getIsSuspended());

		return userDTO;
	}

	@Override
	public List<UserDTO> mapToDTOs(List<User> users) {
		return users.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
