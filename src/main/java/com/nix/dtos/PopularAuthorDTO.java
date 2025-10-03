package com.nix.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularAuthorDTO {
	private UUID id;
	private String username;
	private String fullName;
	private Long totalBooks;
	private Long totalViews;
	private Long followerCount;
	private BigDecimal totalEarnings;
}