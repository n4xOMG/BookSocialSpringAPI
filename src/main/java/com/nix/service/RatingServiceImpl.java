package com.nix.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Book;
import com.nix.models.Rating;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.RatingRepository;

@Service
public class RatingServiceImpl implements RatingService {

	@Autowired
	RatingRepository ratingRepo;

	@Autowired
	UserService userService;

	@Autowired
	BookRepository bookRepository;

	@Override
	public Rating findRatingById(Long ratingId) throws Exception {
		Optional<Rating> rating = ratingRepo.findById(ratingId);
		if (rating == null) {
			throw new Exception("No rating found!");
		}
		return rating.get();
	}

	@Override
	public Double getAverageBookRating(Long bookId) {
		return ratingRepo.getAverageRatingByBookId(bookId);

	}

	@Override
	@Transactional
	public Rating addNewRating(User user, Long bookId, Integer rating) throws Exception {

		Optional<Book> book = bookRepository.findById(bookId);

		Rating newRating = new Rating();

		newRating.setBook(book.get());
		newRating.setUser(user);
		newRating.setRating(rating);

		book.get().getRatings().add(newRating);

		return newRating;
	}

	@Override
	public Rating editRating(Long ratingId, Rating rating) throws Exception {

		Rating editRating = findRatingById(ratingId);

		editRating.setRating(rating.getRating());

		return ratingRepo.save(editRating);
	}

	@Override
	public List<Rating> getAllRatingsForBook(Long bookId) {
		return ratingRepo.findByBookId(bookId);
	}

	@Override
	public List<Rating> getAllRatingsByUser(Long userId) {
		return ratingRepo.findByUserId(userId);
	}

	@Override
	public Rating findRatingByUserAndBook(Long userId, Long bookId) {
		return ratingRepo.findRatingByBookAndUserId(bookId, userId);
	}

}
