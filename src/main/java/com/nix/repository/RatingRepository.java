package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Rating;

public interface RatingRepository extends JpaRepository<Rating, Integer> {
	
	public List<Rating> findByBookId(Integer bookId);
	
	public List<Rating> findByUserId(Integer userId);
	
	@Query("SELECT AVG(r.rating) FROM Rating r WHERE r.book.id = :bookId")
	public Double getAverageRatingByBookId(@Param(value = "bookId") Integer bookId);
	
	@Query("select r from Rating r where r.book.id=:bookId and r.user.id=:userId")
	public Rating findRatingByBookAndUserId(@Param(value="bookId")Integer bookId, @Param(value="userId")Integer userId);
}
