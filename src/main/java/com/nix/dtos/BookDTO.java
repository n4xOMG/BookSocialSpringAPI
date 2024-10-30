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
public class BookDTO {
	private Integer id;
	private String title;
	private UserSummaryDTO author;
	private String artistName;
	private String bookCover;
	private String description;
	private String language;
	private boolean isSuggested;
	private long viewCount;
	private LocalDateTime uploadDate;
	private List<CategoryDTO> categories = new ArrayList<>();
	private Integer favCount;
	private Double avgRating;
	private Integer ratingCount;
	private int chapterCount;
	private String latestChapterNumber;

}
