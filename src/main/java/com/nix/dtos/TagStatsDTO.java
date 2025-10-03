package com.nix.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagStatsDTO {
	private String categoryName;
	private Long bookCount;
	private Long totalViews;
	private Double percentage;
}