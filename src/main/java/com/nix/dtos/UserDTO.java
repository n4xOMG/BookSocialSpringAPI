package com.nix.dtos;

import java.time.LocalDate;
import java.util.ArrayList;
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
	private Integer id;
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
	private List<BookDTO> book = new ArrayList<>();
	private List<CommentDTO> comment = new ArrayList<>();
	private List<PostDTO> post = new ArrayList<>();
}
