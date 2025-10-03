package com.nix.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentAnalyticsDTO {
	private Long totalBooks;
	private Long totalChapters;
	private Long totalUnlocks; // Chapter unlocks
	private List<PopularBookDTO> popularBooks;
	private List<PopularChapterDTO> popularChapters;
	private List<PopularAuthorDTO> popularAuthors;
	private List<CategoryStatsDTO> categoryStats;
}