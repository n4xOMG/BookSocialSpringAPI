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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.mappers.TagMapper;
import com.nix.models.Tag;
import com.nix.models.User;
import com.nix.service.TagService;
import com.nix.service.UserService;

@RestController
public class TagController {

	@Autowired
	TagService tagService;

	@Autowired
	UserService userService;

	TagMapper tagMapper = new TagMapper();

	@GetMapping("/books/tags")
	public ResponseEntity<?> getAllTags() {
		List<Tag> tags = tagService.findAllTags();
		return ResponseEntity.ok(tagMapper.mapToDTOs(tags));
	}

	@GetMapping("/books/tags/{tagId}")
	public ResponseEntity<?> getTagById(@PathVariable Integer tagId) {
		try {
			Tag tag = tagService.findTagById(tagId);
			return ResponseEntity.ok(tagMapper.mapToDTO(tag));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping("/books/{bookId}/tags")
	public ResponseEntity<?> getTagsByBook(@PathVariable Integer bookId) throws Exception {
		try {
			List<Tag> tags = tagService.findALlTagsByBookId(bookId);

			return ResponseEntity.ok(tagMapper.mapToDTOs(tags));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping("/admin/books/tags")
	public ResponseEntity<?> addNewTag(@RequestHeader("Authorization") String jwt, @RequestBody Tag tag)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (!user.getRole().getName().equals("ADMIN") && !user.getRole().getName().equals("TRANSLATOR")) {
			return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
		}
		try {
			Tag newTag = tagService.addNewTag(tag);

			return ResponseEntity.ok(tagMapper.mapToDTO(newTag));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PutMapping("/admin/books/tags/{tagId}")
	public ResponseEntity<?> editTag(@RequestHeader("Authorization") String jwt, @PathVariable Integer tagId,
			@RequestBody Tag tag) throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (!user.getRole().getName().equals("ADMIN") && !user.getRole().getName().equals("TRANSLATOR")) {
			return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
		}
		try {
			Tag editTag = tagService.editTag(tagId, tag);

			return ResponseEntity.ok(tagMapper.mapToDTO(editTag));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/admin/books/tags/{tagId}")
	public ResponseEntity<?> deleteTag(@RequestHeader("Authorization") String jwt, @PathVariable Integer tagId)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (!user.getRole().getName().equals("ADMIN") && !user.getRole().getName().equals("TRANSLATOR")) {
			return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
		}
		try {
			return ResponseEntity.ok(tagService.deleteTag(tagId));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
}
