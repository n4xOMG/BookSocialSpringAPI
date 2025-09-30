package com.nix.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "author_payout_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorPayoutSettings implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@UuidGenerator
	private UUID id;

	@OneToOne
	@JoinColumn(name = "author_id", nullable = false, unique = true)
	private User author;

	@Column(precision = 10, scale = 2)
	private BigDecimal minimumPayoutAmount = BigDecimal.valueOf(25.00); // Minimum amount for payout

	@Enumerated(EnumType.STRING)
	private PayoutFrequency payoutFrequency = PayoutFrequency.MONTHLY;

	// Provider-agnostic payout identifiers
	private String stripeAccountId; // legacy, kept for migration
	private boolean isStripeAccountVerified = false; // legacy, kept for migration

	private String paypalEmail; // PayPal account email for payouts

	private boolean autoPayoutEnabled = true; // Automatically process payouts when minimum reached

	private LocalDateTime lastPayoutDate;

	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;

	// Bank/payment method details (encrypted in production)
	private String paymentMethodType; // "bank_account", "debit_card", etc.
	private String accountHolderName;
	private String bankName;
	private String accountLastFour; // Last 4 digits for display

	public enum PayoutFrequency {
		WEEKLY, MONTHLY, QUARTERLY, MANUAL // Only payout when requested
	}

	// Constructor for new author
	public AuthorPayoutSettings(User author) {
		this.author = author;
		this.createdDate = LocalDateTime.now();
		this.updatedDate = LocalDateTime.now();
	}
}