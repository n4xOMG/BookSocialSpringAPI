package com.nix.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.TagDTO;
import com.nix.dtos.mappers.TagMapper;
import com.nix.models.Tag;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.TagService;
import com.nix.service.UserService;

@RestController
public class TagController {

	private static final Logger logger = LoggerFactory.getLogger(TagController.class);

	@Autowired
	TagService tagService;

	@Autowired
	UserService userService;

	@Autowired
	TagMapper tagMapper;

	@GetMapping("/books/tags")
	public ResponseEntity<ApiResponseWithData<List<TagDTO>>> getAllTags() {
		try {
			List<Tag> tags = tagService.findAllTags();
			return buildSuccessResponse("Tags retrieved successfully.", tagMapper.mapToDTOs(tags));
		} catch (Exception e) {
			logger.error("Failed to retrieve tags", e);
			return this.<List<TagDTO>>buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve tags.");
		}
	}

	@GetMapping("/books/tags/{tagId}")
	public ResponseEntity<ApiResponseWithData<TagDTO>> getTagById(@PathVariable Integer tagId) {
		try {
			Tag tag = tagService.findTagById(tagId);
			if (tag == null) {
				return this.<TagDTO>buildErrorResponse(HttpStatus.NOT_FOUND, "Tag not found.");
			}
			return buildSuccessResponse("Tag retrieved successfully.", tagMapper.mapToDTO(tag));
		} catch (Exception e) {
			logger.error("Failed to retrieve tag {}", tagId, e);
			return this.<TagDTO>buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve tag.");
		}
	}

	@GetMapping("/books/{bookId}/tags")
	public ResponseEntity<ApiResponseWithData<List<TagDTO>>> getTagsByBook(@PathVariable UUID bookId) {
		try {
			List<Tag> tags = tagService.findALlTagsByBookId(bookId);
			return buildSuccessResponse("Book tags retrieved successfully.", tagMapper.mapToDTOs(tags));
		} catch (Exception e) {
			logger.error("Failed to retrieve tags for book {}", bookId, e);
			return this.<List<TagDTO>>buildErrorResponse(HttpStatus.NOT_FOUND,
					"Failed to retrieve tags for the specified book.");
		}
	}

	@PostMapping("/admin/books/tags")
	@PreAuthorize("hasAnyRole('ADMIN', 'TRANSLATOR')")
	public ResponseEntity<ApiResponseWithData<TagDTO>> addNewTag(@RequestHeader("Authorization") String jwt,
			@RequestBody Tag tag) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return this.<TagDTO>buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}
			Tag newTag = tagService.addNewTag(tag);
			return buildSuccessResponse(HttpStatus.CREATED, "Tag created successfully.", tagMapper.mapToDTO(newTag));
		} catch (Exception e) {
			logger.error("Failed to create tag", e);
			return this.<TagDTO>buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@PutMapping("/admin/books/tags/{tagId}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<TagDTO>> editTag(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer tagId, @RequestBody Tag tag) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return this.<TagDTO>buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}
			Tag editTag = tagService.editTag(tagId, tag);

			return buildSuccessResponse("Tag updated successfully.", tagMapper.mapToDTO(editTag));
		} catch (Exception e) {
			logger.error("Failed to update tag {}", tagId, e);
			return this.<TagDTO>buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@DeleteMapping("/admin/books/tags/{tagId}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<String>> deleteTag(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer tagId) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return this.<String>buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found.");
			}
			String result = tagService.deleteTag(tagId);
			return buildSuccessResponse("Tag deleted successfully.", result);
		} catch (Exception e) {
			logger.error("Failed to delete tag {}", tagId, e);
			return this.<String>buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
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
