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

	// Find by Stripe account ID
	Optional<AuthorPayoutSettings> findByStripeAccountId(String stripeAccountId);

	// Check if author has payout settings configured
	boolean existsByAuthor(User author);

	// Check if Stripe account is verified
	boolean existsByAuthorAndIsStripeAccountVerifiedTrue(User author);
}