package com.nix.service;

import java.util.List;
import java.util.UUID;

import com.nix.models.Rating;
import com.nix.models.User;

public interface RatingService {
	public Rating findRatingById(UUID ratingId) throws Exception;

	public Double getAverageBookRating(UUID bookId);

	public Rating addNewRating(User user, UUID bookId, Integer rating) throws Exception;

	public Rating editRating(UUID ratingId, Rating rating) throws Exception;

	public List<Rating> getAllRatingsForBook(UUID bookId);

	public List<Rating> getAllRatingsByUser(UUID userId);

	public Rating findRatingByUserAndBook(UUID userId, UUID bookId);

}
