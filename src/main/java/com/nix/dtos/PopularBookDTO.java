package com.nix.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularBookDTO {
	private UUID id;
	private String title;
	private String authorName;
	private Long viewCount;
	private Long unlockCount; // Number of chapter unlocks
	private Double rating;
	private Long favoriteCount;
}