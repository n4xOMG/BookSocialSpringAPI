package com.nix.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.AuthorDashboardDTO;
import com.nix.dtos.AuthorEarningDTO;
import com.nix.dtos.AuthorPayoutDTO;
import com.nix.dtos.AuthorPayoutSettingsDTO;
import com.nix.dtos.BookDTO;
import com.nix.dtos.BookPerformanceDTO;
import com.nix.models.AuthorPayoutSettings;
import com.nix.models.User;
import com.nix.request.PayoutRequestDTO;
import com.nix.response.ApiResponseWithData;
import com.nix.service.AuthorService;
import com.nix.service.BookService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/api/author")
public class AuthorDashboardController {

	@Autowired
	private UserService userService;

	@Autowired
	private AuthorService authorService;

	@Autowired
	private BookService bookService;

	/**
	 * Get author dashboard overview
	 */
	@GetMapping("/dashboard")
	public ResponseEntity<ApiResponseWithData<AuthorDashboardDTO>> getAuthorDashboard(
			@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
			AuthorDashboardDTO dashboard = authorService.getAuthorDashboard(author, pageable);
			return buildSuccessResponse("Author dashboard retrieved successfully.", dashboard);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error fetching dashboard: " + e.getMessage());
		}
	}

	/**
	 * Get author earnings with pagination
	 */
	@GetMapping("/earnings")
	public ResponseEntity<ApiResponseWithData<Page<AuthorEarningDTO>>> getAuthorEarnings(
			@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			Pageable pageable = PageRequest.of(page, size);
			Page<AuthorEarningDTO> earnings = authorService.getAuthorEarnings(author, pageable);
			return buildSuccessResponse("Author earnings retrieved successfully.", earnings);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error fetching earnings: " + e.getMessage());
		}
	}

	/**
	 * Get author payout history
	 */
	@GetMapping("/payouts")
	public ResponseEntity<ApiResponseWithData<Page<AuthorPayoutDTO>>> getAuthorPayouts(
			@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			Pageable pageable = PageRequest.of(page, size);
			Page<AuthorPayoutDTO> payouts = authorService.getAuthorPayouts(author, pageable);
			return buildSuccessResponse("Author payouts retrieved successfully.", payouts);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error fetching payouts: " + e.getMessage());
		}
	}

	/**
	 * Request a payout
	 */
	@PostMapping("/payouts/request")
	public ResponseEntity<ApiResponseWithData<Void>> requestPayout(@RequestHeader("Authorization") String jwt,
			@RequestBody PayoutRequestDTO payoutRequest) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			// Validate payout amount
			BigDecimal availableBalance = authorService.getUnpaidEarnings(author);
			if (payoutRequest.getAmount().compareTo(availableBalance) > 0) {
				return buildErrorResponse(HttpStatus.BAD_REQUEST,
						"Requested amount exceeds available balance.");
			}

			if (!authorService.canRequestPayout(author)) {
				return buildErrorResponse(HttpStatus.BAD_REQUEST,
						"Payout requirements not met. Check minimum amount and Stripe connection.");
			}

			var payout = authorService.requestPayout(author, payoutRequest.getAmount());
			return buildSuccessResponse("Payout requested successfully. Payout ID: " + payout.getId(), null);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error requesting payout: " + e.getMessage());
		}
	}

	/**
	 * Update payout settings
	 */
	@PutMapping("/payout-settings")
	public ResponseEntity<ApiResponseWithData<AuthorPayoutSettingsDTO>> updatePayoutSettings(
			@RequestHeader("Authorization") String jwt,
			@RequestBody AuthorPayoutSettingsDTO dto) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			AuthorPayoutSettings toUpdate = new AuthorPayoutSettings();
			toUpdate.setMinimumPayoutAmount(dto.getMinimumPayoutAmount());
			toUpdate.setPayoutFrequency(dto.getPayoutFrequency());
			toUpdate.setAutoPayoutEnabled(dto.isAutoPayoutEnabled());
			toUpdate.setPaypalEmail(dto.getPaypalEmail());
			toUpdate.setPaymentMethodType(dto.getPaymentMethodType());
			toUpdate.setAccountHolderName(dto.getAccountHolderName());
			toUpdate.setBankName(dto.getBankName());
			toUpdate.setAccountLastFour(dto.getAccountLastFour());

			AuthorPayoutSettingsDTO updated = authorService.updatePayoutSettings(author, toUpdate);
			return buildSuccessResponse("Payout settings updated successfully.", updated);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error updating payout settings: " + e.getMessage());
		}
	}

	/**
	 * Get payout settings
	 */
	@GetMapping("/payout-settings")
	public ResponseEntity<ApiResponseWithData<AuthorPayoutSettingsDTO>> getPayoutSettings(
			@RequestHeader("Authorization") String jwt) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			AuthorPayoutSettingsDTO settings = authorService.getPayoutSettings(author);
			return buildSuccessResponse("Payout settings retrieved successfully.", settings);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error fetching payout settings: " + e.getMessage());
		}
	}

	/**
	 * Get detailed book performance metrics for the author
	 */
	@GetMapping("/books/performance")
	public ResponseEntity<ApiResponseWithData<List<BookPerformanceDTO>>> getAuthorBookPerformance(
			@RequestHeader("Authorization") String jwt) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			List<BookPerformanceDTO> performance = bookService.getAuthorBookPerformance(author.getId());
			return buildSuccessResponse("Book performance retrieved successfully.", performance);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error fetching book performance: " + e.getMessage());
		}
	}

	@GetMapping("/books/search")
	public ResponseEntity<ApiResponseWithData<Page<BookDTO>>> searchAuthorBooks(
			@RequestHeader("Authorization") String jwt,
			@RequestParam(required = false) String query, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy) {
		try {
			User author = userService.findUserByJwt(jwt);
			if (author == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
			Page<BookDTO> books = bookService.searchBooksForAuthor(author.getId(), query, pageable);
			return buildSuccessResponse("Books retrieved successfully.", books);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Error searching books: " + e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}
}