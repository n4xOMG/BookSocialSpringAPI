package com.nix.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nix.models.AuthorPayout;
import com.nix.repository.AuthorPayoutRepository;

@Service
public class PayoutSchedulerService {

	@Autowired
	private AuthorServiceImpl authorService;

	@Autowired
	private AuthorPayoutRepository authorPayoutRepository;

	/**
	 * Process pending payouts every hour
	 */
	@Scheduled(fixedRate = 3600000) // 1 hour
	public void processPendingPayouts() {
		try {
			List<AuthorPayout> pendingPayouts = authorPayoutRepository.findPendingPayoutsForProcessing();

			for (AuthorPayout payout : pendingPayouts) {
				try {
					authorService.processStripeTransfer(payout);
				} catch (Exception e) {
					payout.setStatus(AuthorPayout.PayoutStatus.FAILED);
					payout.setFailureReason(e.getMessage());
					authorPayoutRepository.save(payout);
					System.err.println("Failed to process payout " + payout.getId() + ": " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("Error processing pending payouts: " + e.getMessage());
		}
	}
}