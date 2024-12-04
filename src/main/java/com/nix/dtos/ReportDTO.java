package com.nix.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportDTO {
	private Long id;
	private String reason;
	private UserSummaryDTO reporter;
	private BookDTO book;
	private ChapterSummaryDTO chapter;
	private CommentDTO comment;
	private LocalDateTime reportedDate;
	private boolean isResolved;
}
