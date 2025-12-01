package com.nix.dtos;

import java.time.LocalDateTime;
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
public class BookDTO {
	private UUID id;
	private String title;
	private UserSummaryDTO author;
	private String authorName;
	private String artistName;
	private ImageAttachmentDTO bookCover;
	private String description;
	private String language;
	private boolean isSuggested; // return suggested
	private long viewCount;
	private String status;
	private LocalDateTime uploadDate;
	private Integer categoryId;
	private String categoryName;
	private List<Integer> tagIds;
	private List<String> tagNames;
	private Integer favCount;
	private Double avgRating;
	private Integer ratingCount;
	private int chapterCount;
	private String latestChapterNumber;
	private boolean isFollowedByCurrentUser = false;

}
