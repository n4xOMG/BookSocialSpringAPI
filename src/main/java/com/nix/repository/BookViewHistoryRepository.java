package com.nix.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Book;
import com.nix.models.BookViewHistory;

public interface BookViewHistoryRepository extends JpaRepository<BookViewHistory, UUID> {

	@Query("SELECT bvh FROM BookViewHistory bvh WHERE bvh.book = :book AND DATE(bvh.viewDate) = DATE(:date)")
	Optional<BookViewHistory> findByBookAndDate(@Param("book") Book book, @Param("date") LocalDateTime date);

	@Query("SELECT bvh FROM BookViewHistory bvh WHERE bvh.viewDate >= :startDate AND bvh.viewDate <= :endDate")
	List<BookViewHistory> findViewsBetweenDates(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT COALESCE(SUM(bvh.dailyViewCount), 0) FROM BookViewHistory bvh WHERE bvh.book = :book AND bvh.viewDate >= :startDate")
	long getViewCountSince(@Param("book") Book book, @Param("startDate") LocalDateTime startDate);

	@Query("SELECT bvh.book FROM BookViewHistory bvh WHERE bvh.viewDate >= :since GROUP BY bvh.book HAVING SUM(bvh.dailyViewCount) >= :minViews ORDER BY SUM(bvh.dailyViewCount) DESC")
	List<Book> findTrendingBooks(@Param("since") LocalDateTime since, @Param("minViews") long minViews,
			Pageable pageable);

	@Query("SELECT COALESCE(SUM(bvh.dailyViewCount), 0) FROM BookViewHistory bvh WHERE bvh.book.id = :bookId AND bvh.viewDate >= :startDate AND bvh.viewDate <= :endDate")
	long getViewCountForBookBetweenDates(@Param("bookId") UUID bookId, @Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Modifying
	@Transactional
	@Query("DELETE FROM BookViewHistory bvh WHERE bvh.book = :book")
	void deleteByBook(@Param("book") Book book);
}