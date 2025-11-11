package com.nix.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "author_payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorPayout implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_CURRENCY = "USD";

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal totalAmount; // Total amount being paid out

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal platformFeesDeducted; // Total platform fees from this payout

	@Column(length = 3, nullable = false, columnDefinition = "varchar(3) default 'USD'")
	private String currency = DEFAULT_CURRENCY;

	private LocalDateTime requestedDate; // When payout was requested
	private LocalDateTime processedDate; // When payout was processed
	private LocalDateTime completedDate; // When payout was completed by payment provider

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PayoutStatus status = PayoutStatus.PENDING;

	private String providerPayoutId; // External provider payout ID for tracking (PayPal)

	private String failureReason; // If payout failed, reason why

	@Column(columnDefinition = "TEXT")
	private String notes; // Additional notes about the payout

	@JsonIgnore
	@OneToMany(mappedBy = "payout", cascade = CascadeType.ALL)
	private List<AuthorEarning> earnings = new ArrayList<>(); // Earnings included in this payout

	// Constructor for creating new payout
	public AuthorPayout(User author, BigDecimal totalAmount, BigDecimal platformFeesDeducted) {
		this(author, totalAmount, platformFeesDeducted, DEFAULT_CURRENCY);
	}

	public AuthorPayout(User author, BigDecimal totalAmount, BigDecimal platformFeesDeducted, String currency) {
		this.author = author;
		this.totalAmount = scaleCurrency(totalAmount);
		this.platformFeesDeducted = scaleCurrency(platformFeesDeducted);
		setCurrency(currency);
		this.requestedDate = LocalDateTime.now();
		this.status = PayoutStatus.PENDING;
	}

	private BigDecimal scaleCurrency(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	public void setCurrency(String currency) {
		this.currency = (currency == null || currency.isBlank()) ? DEFAULT_CURRENCY : currency.toUpperCase();
	}

	public enum PayoutStatus {
		PENDING, // Payout requested but not yet processed
		PROCESSING, // Being processed by provider
		COMPLETED, // Successfully completed
		FAILED, // Failed to process
		CANCELLED // Cancelled by admin or user
	}
}