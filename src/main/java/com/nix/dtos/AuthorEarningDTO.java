package com.nix.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorEarningDTO {
	private UUID id;
	private UUID chapterId;
	private String chapterTitle;
	private String chapterNumber;
	private UUID bookId;
	private String bookTitle;
	private BigDecimal grossAmount;
	private BigDecimal platformFee;
	private BigDecimal netAmount;
	private BigDecimal platformFeePercentage;
	private String currency;
	private LocalDateTime earnedDate;
	private boolean isPaidOut;
	private UUID payoutId;
}