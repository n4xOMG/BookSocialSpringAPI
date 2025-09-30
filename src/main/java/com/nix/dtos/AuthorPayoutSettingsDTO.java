package com.nix.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.nix.models.AuthorPayoutSettings.PayoutFrequency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorPayoutSettingsDTO {
	private UUID id;
	private UUID authorId;
	private BigDecimal minimumPayoutAmount;
	private PayoutFrequency payoutFrequency;
	private String paypalEmail; // preferred payout method
	private boolean autoPayoutEnabled;
	private LocalDateTime lastPayoutDate;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
	private String paymentMethodType;
	private String accountHolderName;
	private String bankName;
	private String accountLastFour;
}
