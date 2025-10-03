package com.nix.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Book;
import com.nix.models.Rating;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.RatingRepository;
import com.nix.service.RatingService;
import com.nix.service.UserService;

@Service
public class RatingServiceImpl implements RatingService {

	@Autowired
	RatingRepository ratingRepo;

	@Autowired
	UserService userService;

	@Autowired
	BookRepository bookRepository;

	@Override
	public Rating findRatingById(UUID ratingId) throws Exception {
		Optional<Rating> rating = ratingRepo.findById(ratingId);
		if (rating == null) {
			throw new Exception("No rating found!");
		}
		return rating.get();
	}

	@Override
	public Double getAverageBookRating(UUID bookId) {
		return ratingRepo.getAverageRatingByBookId(bookId);

	}

	@Override
	@Transactional
	public Rating addNewRating(User user, UUID bookId, Integer rating) throws Exception {

		Optional<Book> book = bookRepository.findById(bookId);

		Rating newRating = new Rating();

		newRating.setBook(book.get());
		newRating.setUser(user);
		newRating.setRating(rating);

		book.get().getRatings().add(newRating);

		return newRating;
	}

	@Override
	public Rating editRating(UUID ratingId, Rating rating) throws Exception {

		Rating editRating = findRatingById(ratingId);

		editRating.setRating(rating.getRating());

		return ratingRepo.save(editRating);
	}

	@Override
	public List<Rating> getAllRatingsForBook(UUID bookId) {
		return ratingRepo.findByBookId(bookId);
	}

	@Override
	public List<Rating> getAllRatingsByUser(UUID userId) {
		return ratingRepo.findByUserId(userId);
	}

	@Override
	public Rating findRatingByUserAndBook(UUID userId, UUID bookId) {
		return ratingRepo.findRatingByBookAndUserId(bookId, userId);
	}

}
