package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	public User findByEmail(String email);

	@Query("select u from User u where u.username LIKE %:username%")
	public List<User> findByUsername(String username);

	@Query("SELECT u FROM User u WHERE u.verificationCode = :code")
	public User findByVerificationCode(String code);

	@Modifying
	@Query("UPDATE User u SET u.credits = :credits WHERE u.id = :userId")
	void updateCredits(@Param("userId") UUID userId, @Param("credits") int credits);

	Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email,
			Pageable pageable);

	// Count all users
	long count();

	// Count banned users
	@Query("SELECT COUNT(u) FROM User u WHERE u.isBanned = true")
	long countBannedUsers();

	// Count suspended users
	@Query("SELECT COUNT(u) FROM User u WHERE u.isSuspended = true")
	long countSuspendedUsers();
}
