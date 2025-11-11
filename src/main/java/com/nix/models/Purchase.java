package com.nix.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.nix.enums.PaymentProvider;
import com.nix.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Purchase implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@UuidGenerator
	private UUID id;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal amount;

	private LocalDateTime purchaseDate;
	private String paymentIntentId; // For Stripe or PayPal transaction ID

	@Enumerated(EnumType.STRING)
	private PaymentProvider paymentProvider;

	@Enumerated(EnumType.STRING)
	private PaymentStatus status;

	@Column(length = 3, nullable = false)
	private String currency = "USD"; // Default currency

	@Column(nullable = false)
	private int creditsPurchased;

	@ManyToOne
	private User user;

	@ManyToOne
	private CreditPackage creditPackage;

}
