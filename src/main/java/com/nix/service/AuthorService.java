package com.nix.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.AuthorDashboardDTO;
import com.nix.dtos.AuthorEarningDTO;
import com.nix.dtos.AuthorPayoutDTO;
import com.nix.dtos.AuthorPayoutSettingsDTO;
import com.nix.models.AuthorPayout;
import com.nix.models.AuthorPayoutSettings;
import com.nix.models.ChapterUnlockRecord;
import com.nix.models.User;

public interface AuthorService {

	// Earnings methods
	void recordChapterUnlockEarning(ChapterUnlockRecord unlockRecord);

	AuthorDashboardDTO getAuthorDashboard(User author, Pageable pageable);

	Page<AuthorEarningDTO> getAuthorEarnings(User author, Pageable pageable);

	BigDecimal getUnpaidEarnings(User author);

	BigDecimal getLifetimeEarnings(User author);

	// Payout methods
	AuthorPayoutDTO requestPayout(User author, BigDecimal amount) throws Exception;

	Page<AuthorPayoutDTO> getAuthorPayouts(User author, Pageable pageable);

	AuthorPayoutSettingsDTO getPayoutSettings(User author);

	AuthorPayoutSettingsDTO updatePayoutSettings(User author, AuthorPayoutSettings settings);

	boolean canRequestPayout(User author);

	// Platform configuration
	BigDecimal getPlatformFeePercentage();

	Page<AuthorPayoutDTO> listPayouts(AuthorPayout.PayoutStatus status, Pageable pageable);

	AuthorPayoutDTO processPayout(UUID payoutId) throws Exception;
}