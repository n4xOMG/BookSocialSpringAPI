package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Rating;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
	
	public List<Rating> findByBookId(UUID bookId);
	
	public List<Rating> findByUserId(UUID userId);
	
	@Query("SELECT AVG(r.rating) FROM Rating r WHERE r.book.id = :bookId")
	public Double getAverageRatingByBookId(@Param(value = "bookId") UUID bookId);
	
	@Query("select r from Rating r where r.book.id=:bookId and r.user.id=:userId")
	public Rating findRatingByBookAndUserId(@Param(value="bookId")UUID bookId, @Param(value="userId")UUID userId);
}
