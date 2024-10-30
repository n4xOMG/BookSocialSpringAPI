package com.nix.controller;

import java.util.List;

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
import com.nix.service.SensitiveWordService;

@RestController
public class SensitiveWordController {
	@Autowired
	SensitiveWordService sensitiveWordService;

	@GetMapping("/translator/sensitive-words")
	public ResponseEntity<?> getAllSensitiveWord() {
		try {
			List<SensitiveWord> words = sensitiveWordService.findAllWords();
			return new ResponseEntity<>(words, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("/translator/sensitive-words/search")
	public ResponseEntity<?> searchSensitiveWord(@RequestParam String query) {
		try {
			List<SensitiveWord> words = sensitiveWordService.findWordByName(query);
			return new ResponseEntity<>(words, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@PostMapping("/translator/sensitive-words")
	public ResponseEntity<?> addSensitiveWord(@RequestBody SensitiveWord sensitiveWord) {
		try {
			SensitiveWord newWord = sensitiveWordService.addNewWord(sensitiveWord);
			return new ResponseEntity<>(newWord, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@PutMapping("/translator/sensitive-words/{wordId}")
	public ResponseEntity<?> editSensitiveWord(@PathVariable Integer wordId, @RequestBody SensitiveWord sensitiveWord) {
		try {
			SensitiveWord editWord = sensitiveWordService.editNewWord(wordId, sensitiveWord);
			return new ResponseEntity<>(editWord, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@DeleteMapping("/translator/sensitive-words/{wordId}")
	public ResponseEntity<?> deleteSensitiveWord(@PathVariable Integer wordId) {
		try {
			return new ResponseEntity<>(sensitiveWordService.deleteWord(wordId), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

}
