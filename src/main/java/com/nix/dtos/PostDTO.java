package com.nix.dtos;

import java.time.LocalDateTime;
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
public class PostDTO {
	private Integer id;
	private UserSummaryDTO user;
	private String content;
	private List<String> images = new ArrayList<>();
	private Integer likes;
	private Integer comments;
	private LocalDateTime timestamp;
}
