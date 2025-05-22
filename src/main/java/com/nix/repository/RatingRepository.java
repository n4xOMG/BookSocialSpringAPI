package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {
	
	public List<Rating> findByBookId(Long bookId);
	
	public List<Rating> findByUserId(Long userId);
	
	@Query("SELECT AVG(r.rating) FROM Rating r WHERE r.book.id = :bookId")
	public Double getAverageRatingByBookId(@Param(value = "bookId") Long bookId);
	
	@Query("select r from Rating r where r.book.id=:bookId and r.user.id=:userId")
	public Rating findRatingByBookAndUserId(@Param(value="bookId")Long bookId, @Param(value="userId")Long userId);
}
