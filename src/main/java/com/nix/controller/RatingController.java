package com.nix.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.BookDTO;
import com.nix.dtos.RatingDTO;
import com.nix.dtos.mappers.BookMapper;
import com.nix.dtos.mappers.RatingMapper;
import com.nix.models.Rating;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.BookService;
import com.nix.service.RatingService;
import com.nix.service.UserService;

@RestController
public class RatingController {
	private static final Logger logger = LoggerFactory.getLogger(RatingController.class);

	@Autowired
	BookService bookService;

	@Autowired
	UserService userService;

	@Autowired
	RatingService ratingService;

	BookMapper bookMapper = new BookMapper();

	RatingMapper ratingMapper = new RatingMapper();

	@GetMapping("/books/rating/average/{bookId}")
	public ResponseEntity<ApiResponseWithData<Double>> getAverageBookRating(@PathVariable("bookId") UUID bookId) {
		try {
			BookDTO book = bookService.getBookById(bookId);

			if (book == null) {
				return buildErrorResponse(HttpStatus.NOT_FOUND, "Book not found.");
			}

			double averageRating = ratingService.getAverageBookRating(bookId);
			return buildSuccessResponse("Average rating retrieved successfully.", averageRating);
		} catch (Exception e) {
			logger.error("Failed to retrieve average rating for book {}", bookId, e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve average rating.");
		}
	}

	@GetMapping("/api/books/rating/{bookId}")
	public ResponseEntity<ApiResponseWithData<RatingDTO>> getRatingByBookAndUser(
			@RequestHeader("Authorization") String jwt, @PathVariable("bookId") UUID bookId) {
		try {
			User reqUser = userService.findUserByJwt(jwt);
			if (reqUser == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			BookDTO book = bookService.getBookById(bookId);

			if (book == null) {
				return buildErrorResponse(HttpStatus.NOT_FOUND, "Book not found.");
			}
			Rating rating = ratingService.findRatingByUserAndBook(reqUser.getId(), bookId);
			if (rating == null) {
				return buildErrorResponse(HttpStatus.NOT_FOUND, "Rating not found.");
			}
			return buildSuccessResponse("Rating retrieved successfully.", ratingMapper.mapToDTO(rating));
		} catch (Exception e) {
			logger.error("Error getting rating by book and user", e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve rating.");
		}
	}

	@PatchMapping("/api/books/rating/{bookId}")
	public ResponseEntity<ApiResponseWithData<RatingDTO>> rateBook(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID bookId, @RequestBody Rating rating) {
		try {
			BookDTO book = bookService.getBookById(bookId);
			if (book == null) {
				return buildErrorResponse(HttpStatus.NOT_FOUND, "Book not found.");
			}
			User reqUser = userService.findUserByJwt(jwt);
			if (reqUser == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}
			Rating existingRating = ratingService.findRatingByUserAndBook(reqUser.getId(), bookId);
			if (existingRating == null) {
				RatingDTO created = ratingMapper.mapToDTO(
						ratingService.addNewRating(reqUser, bookId, rating.getRating()));
				return buildSuccessResponse(HttpStatus.CREATED, "Rating created successfully.", created);
			}
			RatingDTO updated = ratingMapper.mapToDTO(ratingService.editRating(existingRating.getId(), rating));
			return buildSuccessResponse("Rating updated successfully.", updated);
		} catch (Exception e) {
			logger.error("Error rating book {}", bookId, e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to rate book.");
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false, null));
	}
}
