package com.nix.controller;

import java.util.UUID;

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
import com.nix.service.BookService;
import com.nix.service.RatingService;
import com.nix.service.UserService;

@RestController
public class RatingController {
	@Autowired
	BookService bookService;

	@Autowired
	UserService userService;

	@Autowired
	RatingService ratingService;

	BookMapper bookMapper = new BookMapper();

	RatingMapper ratingMapper = new RatingMapper();

	@GetMapping("/books/rating/average/{bookId}")
	public ResponseEntity<?> getAverageBookRating(@PathVariable("bookId") UUID bookId) throws Exception {
		try {
			BookDTO book = bookService.getBookById(bookId);

			if (book == null) {
				throw new Exception("Book not found");
			}

			return new ResponseEntity<>(ratingService.getAverageBookRating(bookId), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
		}
	}

	@GetMapping("/api/books/rating/{bookId}")
	public ResponseEntity<?> getRatingByBookAndUser(@RequestHeader("Authorization") String jwt,
			@PathVariable("bookId") UUID bookId) throws Exception {
		try {
			User reqUser = userService.findUserByJwt(jwt);
			if (reqUser == null) {
				throw new Exception("User not found");
			}

			BookDTO book = bookService.getBookById(bookId);

			if (book == null) {
				throw new Exception("Book not found");
			}
			Rating rating = ratingService.findRatingByUserAndBook(reqUser.getId(), bookId);
			return new ResponseEntity<>(ratingMapper.mapToDTO(rating), HttpStatus.OK);
		} catch (Exception e) {
			System.out.println("Error getting rating by book and user: "+e);
			return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
		}
	}

	@PatchMapping("/api/books/rating/{bookId}")
	public ResponseEntity<RatingDTO> rateBook(@RequestHeader("Authorization") String jwt, @PathVariable UUID bookId,
			@RequestBody Rating rating) throws Exception {

		BookDTO book = bookService.getBookById(bookId);
		if (book == null) {
			throw new Exception("Book not found");
		}
		User reqUser = userService.findUserByJwt(jwt);
		if (reqUser == null) {
			throw new Exception("User not found");
		}
		Rating isRatedByUser = ratingService.findRatingByUserAndBook(reqUser.getId(), bookId);
		if (isRatedByUser == null) {
			return new ResponseEntity<>(ratingMapper.mapToDTO(ratingService.addNewRating(reqUser, bookId, rating.getRating())), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(ratingMapper.mapToDTO(ratingService.editRating(isRatedByUser.getId(), rating)),
					HttpStatus.OK);
		}
	}
}
