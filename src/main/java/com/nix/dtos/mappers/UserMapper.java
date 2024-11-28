package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.UserDTO;
import com.nix.models.User;

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
		userDTO.setRole(roleMapper.mapToDTO(user.getRole()));
		userDTO.setCredits(user.getCredits());
		userDTO.setAvatarUrl(user.getAvatarUrl());
		userDTO.setFullname(user.getFullname());
		userDTO.setBio(user.getBio());
		userDTO.setGender(user.getGender());
		userDTO.setBanned(user.isBanned());
		userDTO.setBanReason(user.getBanReason());
		userDTO.setIsVerified(user.getIsVerified());
		userDTO.setIsSuspended(user.getIsSuspended());

		if (user.getFollowedBooks() != null) {
			userDTO.setBook(bookMapper.mapToDTOs(user.getFollowedBooks()));
		}
		if (user.getLikedComments() != null) {
			userDTO.setComment(commentMapper.mapToDTOs(user.getLikedComments()));
		}
		if (user.getLikedPosts() != null) {
			userDTO.setPost(postMapper.mapToDTOs(user.getLikedPosts()));
		}

		return userDTO;
	}

	@Override
	public List<UserDTO> mapToDTOs(List<User> users) {
		return users.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
