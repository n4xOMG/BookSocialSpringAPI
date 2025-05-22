package com.nix.service;

import java.util.List;

import com.nix.models.Rating;
import com.nix.models.User;

public interface RatingService {
	public Rating findRatingById(Long ratingId) throws Exception;

	public Double getAverageBookRating(Long bookId);

	public Rating addNewRating(User user, Long bookId, Integer rating) throws Exception;

	public Rating editRating(Long ratingId, Rating rating) throws Exception;

	public List<Rating> getAllRatingsForBook(Long bookId);

	public List<Rating> getAllRatingsByUser(Long userId);

	public Rating findRatingByUserAndBook(Long userId, Long bookId);

}
