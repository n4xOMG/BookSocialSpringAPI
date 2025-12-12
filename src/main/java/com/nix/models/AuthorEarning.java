package com.nix.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
	private static final String DEFAULT_CURRENCY = "USD";

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@ManyToOne
	@JoinColumn(name = "chapter_id", nullable = true)
	private Chapter chapter;

	@ManyToOne
	@JoinColumn(name = "unlock_record_id", nullable = true)
	private ChapterUnlockRecord unlockRecord;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal grossAmount; // Total amount from chapter unlock

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal platformFee; // Platform fee (5-10%)

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal netAmount; // Author's share after platform fee

	@Column(precision = 5, scale = 2, nullable = false)
	private BigDecimal platformFeePercentage; // Fee percentage at time of earning

	@Column(length = 3, nullable = false, columnDefinition = "varchar(3) default 'USD'")
	private String currency = DEFAULT_CURRENCY; // Currency captured at earning time

	private LocalDateTime earnedDate;

	private boolean isPaidOut = false; // Whether this earning has been paid out

	@ManyToOne
	@JoinColumn(name = "payout_id")
	private AuthorPayout payout; // Reference to payout batch if paid

	// Constructor for creating earnings
	public AuthorEarning(User author, Chapter chapter, ChapterUnlockRecord unlockRecord, BigDecimal grossAmount,
			BigDecimal platformFeePercentage) {
		this(author, chapter, unlockRecord, grossAmount, platformFeePercentage, DEFAULT_CURRENCY);
	}

	public AuthorEarning(User author, Chapter chapter, ChapterUnlockRecord unlockRecord, BigDecimal grossAmount,
			BigDecimal platformFeePercentage, String currency) {
		this.author = author;
		this.chapter = chapter;
		this.unlockRecord = unlockRecord;
		this.grossAmount = scaleCurrency(grossAmount);
		this.platformFeePercentage = platformFeePercentage != null
				? platformFeePercentage.setScale(2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		this.platformFee = calculatePlatformFee(this.grossAmount, this.platformFeePercentage);
		this.netAmount = this.grossAmount.subtract(this.platformFee).setScale(2, RoundingMode.HALF_UP);
		this.earnedDate = LocalDateTime.now();
		this.isPaidOut = false;
		setCurrency(currency);
	}

	private BigDecimal calculatePlatformFee(BigDecimal amount, BigDecimal percentage) {
		if (amount == null || percentage == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal fee = amount.multiply(percentage).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
		return fee.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal scaleCurrency(BigDecimal amount) {
		if (amount == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	public void setCurrency(String currency) {
		if (currency == null || currency.isBlank()) {
			this.currency = DEFAULT_CURRENCY;
		} else {
			this.currency = currency.toUpperCase();
		}
	}
}