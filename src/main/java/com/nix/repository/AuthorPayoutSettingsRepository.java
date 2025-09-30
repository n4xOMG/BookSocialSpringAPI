package com.nix.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nix.models.AuthorPayoutSettings;
import com.nix.models.User;

@Repository
public interface AuthorPayoutSettingsRepository extends JpaRepository<AuthorPayoutSettings, UUID> {

	// Find payout settings by author
	Optional<AuthorPayoutSettings> findByAuthor(User author);

	// Find by provider identifiers (legacy Stripe kept for migration)
	Optional<AuthorPayoutSettings> findByStripeAccountId(String stripeAccountId);

	Optional<AuthorPayoutSettings> findByPaypalEmail(String paypalEmail);

	// Check if author has payout settings configured
	boolean existsByAuthor(User author);

	// Check if PayPal email configured
	boolean existsByAuthorAndPaypalEmailNotNull(User author);
}