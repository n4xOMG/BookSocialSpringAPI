package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.UserSummaryDTO;
import com.nix.models.User;

public class UserSummaryMapper implements Mapper<User, UserSummaryDTO> {

	@Override
	public UserSummaryDTO mapToDTO(User user) {
		UserSummaryDTO userDTO = new UserSummaryDTO();
		userDTO.setId(user.getId());
		userDTO.setUsername(user.getUsername());
		userDTO.setAvatarUrl(user.getAvatarUrl());
		userDTO.setBio(user.getBio());
		return userDTO;
	}

	@Override
	public List<UserSummaryDTO> mapToDTOs(List<User> users) {
		return users.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
