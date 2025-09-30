package com.nix.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "author_earnings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorEarning implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@ManyToOne
	@JoinColumn(name = "chapter_id", nullable = false)
	private Chapter chapter;

	@ManyToOne
	@JoinColumn(name = "unlock_record_id", nullable = false)
	private ChapterUnlockRecord unlockRecord;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal grossAmount; // Total amount from chapter unlock

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal platformFee; // Platform fee (5-10%)

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal netAmount; // Author's share after platform fee

	@Column(precision = 5, scale = 2, nullable = false)
	private BigDecimal platformFeePercentage; // Fee percentage at time of earning

	private LocalDateTime earnedDate;

	private boolean isPaidOut = false; // Whether this earning has been paid out

	@ManyToOne
	@JoinColumn(name = "payout_id")
	private AuthorPayout payout; // Reference to payout batch if paid

	// Constructor for creating earnings
	public AuthorEarning(User author, Chapter chapter, ChapterUnlockRecord unlockRecord, BigDecimal grossAmount,
			BigDecimal platformFeePercentage) {
		this.author = author;
		this.chapter = chapter;
		this.unlockRecord = unlockRecord;
		this.grossAmount = grossAmount;
		this.platformFeePercentage = platformFeePercentage;
		this.platformFee = grossAmount.multiply(platformFeePercentage.divide(BigDecimal.valueOf(100)));
		this.netAmount = grossAmount.subtract(this.platformFee);
		this.earnedDate = LocalDateTime.now();
		this.isPaidOut = false;
	}
}