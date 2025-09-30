package com.nix.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.AuthorDashboardDTO;
import com.nix.dtos.AuthorEarningDTO;
import com.nix.dtos.AuthorPayoutDTO;
import com.nix.models.AuthorEarning;
import com.nix.models.AuthorPayout;
import com.nix.models.AuthorPayoutSettings;
import com.nix.models.ChapterUnlockRecord;
import com.nix.models.User;

public interface AuthorService {

	// Earnings methods
	AuthorEarning recordChapterUnlockEarning(ChapterUnlockRecord unlockRecord);

	AuthorDashboardDTO getAuthorDashboard(User author, Pageable pageable);

	Page<AuthorEarningDTO> getAuthorEarnings(User author, Pageable pageable);

	BigDecimal getUnpaidEarnings(User author);

	BigDecimal getLifetimeEarnings(User author);

	// Payout methods
	AuthorPayout requestPayout(User author, BigDecimal amount) throws Exception;

	Page<AuthorPayoutDTO> getAuthorPayouts(User author, Pageable pageable);

	AuthorPayoutSettings getPayoutSettings(User author);

	AuthorPayoutSettings updatePayoutSettings(User author, AuthorPayoutSettings settings);

	String createStripeConnectAccountLink(User author) throws Exception;

	boolean canRequestPayout(User author);

	// Platform configuration
	BigDecimal getPlatformFeePercentage();
}