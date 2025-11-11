package com.nix.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.nix.models.AuthorPayout.PayoutStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorPayoutDTO {
	private UUID id;
	private UUID authorId;
	private String authorName;
	private BigDecimal totalAmount;
	private BigDecimal platformFeesDeducted;
	private String currency;
	private LocalDateTime requestedDate;
	private LocalDateTime processedDate;
	private LocalDateTime completedDate;
	private PayoutStatus status;
	private String providerPayoutId;
	private String failureReason;
	private String notes;
	private int earningsCount; // Number of earnings included in this payout
}