package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportDTO {
	private UUID id;
	private String reason;
	private UserSummaryDTO reporter;
	private BookDTO book;
	private ChapterSummaryDTO chapter;
	private CommentDTO comment;
	private LocalDateTime reportedDate;
	private boolean isResolved; //return resolved
}
