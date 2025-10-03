package com.nix.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
	private UUID id;
	private String username;
	private String email;
	private String fullname;
	private String gender;
	private LocalDate birthdate;
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
