package com.nix.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nix.models.AuthorPayout;
import com.nix.models.User;

@Repository
public interface AuthorPayoutRepository extends JpaRepository<AuthorPayout, UUID> {

	// Find payouts by author with pagination
	Page<AuthorPayout> findByAuthorOrderByRequestedDateDesc(User author, Pageable pageable);

	// Find payouts by status
	List<AuthorPayout> findByStatusOrderByRequestedDateDesc(AuthorPayout.PayoutStatus status);

	// Find pending payouts for an author
	List<AuthorPayout> findByAuthorAndStatus(User author, AuthorPayout.PayoutStatus status);

	// Find payout by Stripe payout ID
	AuthorPayout findByStripePayoutId(String stripePayoutId);

	// Calculate total amount paid out to an author
	@Query("SELECT COALESCE(SUM(ap.totalAmount), 0) FROM AuthorPayout ap WHERE ap.author = :author AND ap.status = 'COMPLETED'")
	BigDecimal getTotalPaidOutForAuthor(@Param("author") User author);

	// Find all pending payouts for processing
	@Query("SELECT ap FROM AuthorPayout ap WHERE ap.status = 'PENDING' ORDER BY ap.requestedDate ASC")
	List<AuthorPayout> findPendingPayoutsForProcessing();
}