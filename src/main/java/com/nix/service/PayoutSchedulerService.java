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

	@Autowired
	private PayPalPayoutService payPalPayoutService;

	/**
	 * Process pending payouts every hour
	 */
	@Scheduled(fixedRate = 3600000) // 1 hour
	public void processPendingPayouts() {
		try {
			List<AuthorPayout> pendingPayouts = authorPayoutRepository.findPendingPayoutsForProcessing();

			for (AuthorPayout payout : pendingPayouts) {
				try {
					authorService.processPayPalPayout(payout);
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

	/**
	 * Poll processing payouts more frequently and flip to COMPLETED/FAILED when
	 * provider finishes.
	 */
	@Scheduled(fixedRate = 120000) // 20 minutes
	public void pollProcessingPayouts() {
		try {
			List<AuthorPayout> processing = authorPayoutRepository
					.findByStatusOrderByRequestedDateDesc(AuthorPayout.PayoutStatus.PROCESSING);
			for (AuthorPayout payout : processing) {
				try {
					String payoutBatchId = payout.getProviderPayoutId();
					if (payoutBatchId == null || payoutBatchId.isBlank())
						continue;
					PayPalPayoutService.PayPalPayoutStatus status = payPalPayoutService
							.getPayoutBatchStatus(payoutBatchId);
					switch (status) {
					case SUCCESS:
						payout.setStatus(AuthorPayout.PayoutStatus.COMPLETED);
						payout.setCompletedDate(java.time.LocalDateTime.now());
						authorPayoutRepository.save(payout);
						break;
					case FAILED:
						payout.setStatus(AuthorPayout.PayoutStatus.FAILED);
						payout.setFailureReason("Marked failed by provider polling");
						authorPayoutRepository.save(payout);
						break;
					default:
						// still processing or unknown; skip
					}
				} catch (Exception ex) {
					System.err.println("Error polling payout status for " + payout.getId() + ": " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("Error polling processing payouts: " + e.getMessage());
		}
	}
}