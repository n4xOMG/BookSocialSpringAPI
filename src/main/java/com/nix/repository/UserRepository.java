package com.nix.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	public User findByEmail(String email);

	@Query("select u from User u where u.username LIKE %:username%")
	public List<User> findByUsername(String username);

	@Query("SELECT u FROM User u WHERE u.verificationCode = :code")
	public User findByVerificationCode(String code);

	@Query("SELECT u FROM User u WHERE u.emailRecoveryToken = :token")
	public User findByEmailRecoveryToken(@Param("token") String token);

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

	// Analytics queries
	@Query("SELECT COUNT(u) FROM User u WHERE u.accountCreatedDate >= :startDate")
	long countUsersFromDate(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginDate >= :startDate")
	long countActiveUsersFromDate(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT DATE(u.accountCreatedDate), COUNT(u) FROM User u WHERE u.accountCreatedDate >= :startDate GROUP BY DATE(u.accountCreatedDate) ORDER BY DATE(u.accountCreatedDate)")
	List<Object[]> getUserGrowthData(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT u FROM User u WHERE u.role.name = 'AUTHOR' ORDER BY (SELECT COUNT(b) FROM Book b WHERE b.author = u) DESC")
	List<User> findTopAuthorsByBookCount(Pageable pageable);

	@Query("SELECT u FROM User u WHERE u.role.name = 'AUTHOR' ORDER BY (SELECT SUM(ae.netAmount) FROM AuthorEarning ae WHERE ae.author = u) DESC")
	List<User> findTopAuthorsByEarnings(Pageable pageable);
}
