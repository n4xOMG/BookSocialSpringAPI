package com.nix.dtos;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryDTO {
	private Integer id;
	private String username;
	private String email;
	private String fullname;
	private String gender;
	private LocalDate birthdate;
	private String avatarUrl;
	private String bio;
}
