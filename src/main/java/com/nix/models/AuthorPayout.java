package com.nix.models;

import java.io.Serializable;
import java.math.BigDecimal;
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

	private LocalDateTime requestedDate; // When payout was requested
	private LocalDateTime processedDate; // When payout was processed
	private LocalDateTime completedDate; // When payout was completed by Stripe

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PayoutStatus status = PayoutStatus.PENDING;

	private String stripePayoutId; // Stripe payout ID for tracking

	private String failureReason; // If payout failed, reason why

	@Column(columnDefinition = "TEXT")
	private String notes; // Additional notes about the payout

	@JsonIgnore
	@OneToMany(mappedBy = "payout", cascade = CascadeType.ALL)
	private List<AuthorEarning> earnings = new ArrayList<>(); // Earnings included in this payout

	// Constructor for creating new payout
	public AuthorPayout(User author, BigDecimal totalAmount, BigDecimal platformFeesDeducted) {
		this.author = author;
		this.totalAmount = totalAmount;
		this.platformFeesDeducted = platformFeesDeducted;
		this.requestedDate = LocalDateTime.now();
		this.status = PayoutStatus.PENDING;
	}

	public enum PayoutStatus {
		PENDING, // Payout requested but not yet processed
		PROCESSING, // Being processed by Stripe
		COMPLETED, // Successfully completed
		FAILED, // Failed to process
		CANCELLED // Cancelled by admin or user
	}
}