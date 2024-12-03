package com.nix.dtos;

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
	private Integer bookId;
	private Integer chapterId;
	private Integer commentId;
}
