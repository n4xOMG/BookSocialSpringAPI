package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
	private Long id;
	private String username;
	private String email;
	private String fullname;
	private String gender;
	private LocalDateTime birthdate;
	private String avatarUrl;
	private String bio;
	private Boolean isVerified;
	private RoleDTO role;
	private Boolean isSuspended;
	private boolean isBanned;
	private String banReason;
	private int credits;
	private List<CategoryDTO> preferredCategories;
	private List<TagDTO> preferredTags;
}
