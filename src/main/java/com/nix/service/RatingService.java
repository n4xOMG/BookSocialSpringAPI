package com.nix.service;

import java.util.List;

import com.nix.models.Rating;

public interface RatingService {
	public Rating findRatingById(Integer ratingId) throws Exception;

	public Double getAverageBookRating(Integer bookId);

	public Rating addNewRating(Rating rating) throws Exception;

	public Rating editRating(Integer ratingId, Rating rating) throws Exception;

	public List<Rating> getAllRatingsForBook(Integer bookId);

	public List<Rating> getAllRatingsByUser(Integer userId);

	public Rating findRatingByUserAndBook(Integer userId, Integer bookId);

}
