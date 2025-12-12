package com.nix.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nix.models.AuthorEarning;
import com.nix.models.User;

@Repository
public interface AuthorEarningRepository extends JpaRepository<AuthorEarning, UUID> {

	// Find earnings by chapter
	List<AuthorEarning> findByChapterId(UUID chapterId);

	// Find earnings by author with pagination
	Page<AuthorEarning> findByAuthorOrderByEarnedDateDesc(User author, Pageable pageable);

	// Find unpaid earnings for an author
	List<AuthorEarning> findByAuthorAndIsPaidOutFalseOrderByEarnedDateDesc(User author);

	// Calculate total unpaid earnings for an author
	@Query("SELECT COALESCE(SUM(ae.netAmount), 0) FROM AuthorEarning ae WHERE ae.author = :author AND ae.isPaidOut = false")
	BigDecimal getTotalUnpaidEarningsForAuthor(@Param("author") User author);

	// Calculate total lifetime earnings for an author
	@Query("SELECT COALESCE(SUM(ae.netAmount), 0) FROM AuthorEarning ae WHERE ae.author = :author")
	BigDecimal getTotalLifetimeEarningsForAuthor(@Param("author") User author);

	// Calculate earnings for a specific time period
	@Query("SELECT COALESCE(SUM(ae.netAmount), 0) FROM AuthorEarning ae WHERE ae.author = :author AND ae.earnedDate BETWEEN :startDate AND :endDate")
	BigDecimal getEarningsForPeriod(@Param("author") User author, @Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT COALESCE(SUM(ae.netAmount), 0) FROM AuthorEarning ae")
	BigDecimal getTotalAuthorEarnings();

	@Query("SELECT COALESCE(SUM(ae.platformFee), 0) FROM AuthorEarning ae")
	BigDecimal getTotalPlatformFees();

	@Query("SELECT ae.author, SUM(ae.netAmount) FROM AuthorEarning ae GROUP BY ae.author ORDER BY SUM(ae.netAmount) DESC")
	List<Object[]> getTopEarningAuthors(Pageable pageable);
}