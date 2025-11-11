package com.nix.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.models.SensitiveWord;
import com.nix.response.ApiResponseWithData;
import com.nix.service.SensitiveWordService;

@RestController
public class SensitiveWordController {

	private static final Logger logger = LoggerFactory.getLogger(SensitiveWordController.class);
	@Autowired
	SensitiveWordService sensitiveWordService;

	@GetMapping("/translator/sensitive-words")
	public ResponseEntity<ApiResponseWithData<List<SensitiveWord>>> getAllSensitiveWord() {
		try {
			List<SensitiveWord> words = sensitiveWordService.findAllWords();
			return buildSuccessResponse("Sensitive words retrieved successfully.", words);
		} catch (Exception e) {
			logger.error("Failed to retrieve sensitive words", e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve sensitive words.");
		}
	}

	@GetMapping("/translator/sensitive-words/search")
	public ResponseEntity<ApiResponseWithData<List<SensitiveWord>>> searchSensitiveWord(@RequestParam String query) {
		try {
			List<SensitiveWord> words = sensitiveWordService.findWordByName(query);
			return buildSuccessResponse("Sensitive words search completed successfully.", words);
		} catch (Exception e) {
			logger.error("Failed to search sensitive words with query {}", query, e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search sensitive words.");
		}
	}

	@PostMapping("/translator/sensitive-words")
	public ResponseEntity<ApiResponseWithData<SensitiveWord>> addSensitiveWord(
			@RequestBody SensitiveWord sensitiveWord) {
		try {
			SensitiveWord newWord = sensitiveWordService.addNewWord(sensitiveWord);
			return buildSuccessResponse(HttpStatus.CREATED, "Sensitive word created successfully.", newWord);
		} catch (Exception e) {
			logger.error("Failed to create sensitive word", e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create sensitive word.");
		}
	}

	@PutMapping("/translator/sensitive-words/{wordId}")
	public ResponseEntity<ApiResponseWithData<SensitiveWord>> editSensitiveWord(@PathVariable Integer wordId,
			@RequestBody SensitiveWord sensitiveWord) {
		try {
			SensitiveWord editWord = sensitiveWordService.editNewWord(wordId, sensitiveWord);
			return buildSuccessResponse("Sensitive word updated successfully.", editWord);
		} catch (Exception e) {
			logger.error("Failed to update sensitive word {}", wordId, e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update sensitive word.");
		}
	}

	@DeleteMapping("/translator/sensitive-words/{wordId}")
	public ResponseEntity<ApiResponseWithData<String>> deleteSensitiveWord(@PathVariable Integer wordId) {
		try {
			String result = sensitiveWordService.deleteWord(wordId);
			return buildSuccessResponse("Sensitive word deleted successfully.", result);
		} catch (Exception e) {
			logger.error("Failed to delete sensitive word {}", wordId, e);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete sensitive word.");
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
