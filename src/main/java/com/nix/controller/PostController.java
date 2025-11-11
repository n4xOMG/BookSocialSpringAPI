package com.nix.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.PostDTO;
import com.nix.exception.ResourceNotFoundException;
import com.nix.exception.UnauthorizedException;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.PostService;
import com.nix.service.UserService;

@RestController
public class PostController {
	@Autowired
	private PostService postService;

	@Autowired
	private UserService userService;

	/**
	 * Helper method to extract the current user from JWT if present
	 */
	private User getCurrentUser(String jwt) {
		if (jwt != null && !jwt.isEmpty()) {
			return userService.findUserByJwt(jwt);
		}
		return null;
	}

	private void ensureUserCanPublish(User user) {
		if (user == null) {
			throw new ResourceNotFoundException("Cannot find user");
		}
		if (user.isBanned()) {
			throw new UnauthorizedException("Your account is banned. Contact support for assistance.");
		}
		if (Boolean.TRUE.equals(user.getIsSuspended())) {
			throw new UnauthorizedException("Your account is suspended. Contact support for assistance.");
		}
		if (!Boolean.TRUE.equals(user.getIsVerified())) {
			throw new UnauthorizedException("Please verify your account before creating posts.");
		}
	}

	/**
	 * Helper method to parse sort parameter
	 */
	private Sort parseSort(String sort) {
		String[] parts = sort.split(",");
		String property = parts[0];
		Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC
				: Sort.Direction.DESC;
		return Sort.by(direction, property);
	}

	@GetMapping("/posts")
	public ResponseEntity<ApiResponseWithData<Page<PostDTO>>> getAllPosts(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "timestamp,desc") String sort,
			@RequestHeader(value = "Authorization", required = false) String jwt) {

		User currentUser = getCurrentUser(jwt);
		Pageable pageable = PageRequest.of(page, size, parseSort(sort));
		Page<PostDTO> postsPage = postService.getAllPosts(pageable, currentUser);
		return buildSuccessResponse("Posts retrieved successfully.", postsPage);
	}

	@GetMapping("/posts/{postId}")
	public ResponseEntity<ApiResponseWithData<PostDTO>> getPostById(@PathVariable UUID postId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {

		User currentUser = getCurrentUser(jwt);
		PostDTO post = postService.getPostById(postId, currentUser);
		return buildSuccessResponse("Post retrieved successfully.", post);
	}

	@GetMapping("/posts/user/{userId}")
	public ResponseEntity<ApiResponseWithData<List<PostDTO>>> getPostsByUser(@PathVariable("userId") UUID userId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {

		User user = userService.findUserById(userId);
		if (user == null) {
			throw new ResourceNotFoundException("Cannot find user with id: " + userId);
		}

		User currentUser = getCurrentUser(jwt);
		List<PostDTO> posts = postService.getPostsByUser(user, currentUser);
		return buildSuccessResponse("Posts retrieved successfully.", posts);
	}

	@PostMapping("/api/posts")
	public ResponseEntity<ApiResponseWithData<PostDTO>> createPost(@RequestBody PostDTO postDTO,
			@RequestHeader("Authorization") String jwt) {
		User currentUser = getCurrentUser(jwt);
		ensureUserCanPublish(currentUser);
		PostDTO createdPost = postService.createPost(currentUser, postDTO);
		return buildSuccessResponse(HttpStatus.CREATED, "Post created successfully.", createdPost);
	}

	@PutMapping("/api/posts/{id}")
	public ResponseEntity<ApiResponseWithData<PostDTO>> updatePost(@PathVariable UUID id,
			@RequestBody PostDTO postDetails,
			@RequestHeader("Authorization") String jwt) {
		User currentUser = getCurrentUser(jwt);
		ensureUserCanPublish(currentUser);
		PostDTO updatedPost = postService.updatePost(currentUser, id, postDetails);
		return buildSuccessResponse("Post updated successfully.", updatedPost);
	}

	@DeleteMapping("/api/posts/{id}")
	public ResponseEntity<ApiResponseWithData<Void>> deletePost(@PathVariable UUID id,
			@RequestHeader("Authorization") String jwt) {
		User currentUser = getCurrentUser(jwt);
		postService.deletePost(currentUser, id);
		return buildSuccessResponse("Post deleted successfully.", null);
	}

	@PostMapping("/api/posts/{postId}/like")
	public ResponseEntity<ApiResponseWithData<PostDTO>> likePost(@PathVariable UUID postId,
			@RequestHeader("Authorization") String jwt) {
		User currentUser = getCurrentUser(jwt);
		if (currentUser == null) {
			throw new ResourceNotFoundException("Cannot find user");
		}
		PostDTO likedPost = postService.likePost(postId, currentUser);
		return buildSuccessResponse("Post like toggled successfully.", likedPost);
	}

	@PostMapping("/posts/share-chapter/{chapterId}")
	public ResponseEntity<ApiResponseWithData<PostDTO>> shareChapterAsPost(@PathVariable UUID chapterId,
			@RequestBody PostDTO postDTO,
			@RequestHeader("Authorization") String jwt) {

		User currentUser = getCurrentUser(jwt);
		ensureUserCanPublish(currentUser);
		PostDTO post = postService.createChapterSharePost(chapterId, currentUser, postDTO);

		return buildSuccessResponse("Chapter shared successfully.", post);
	}

	@PostMapping("/posts/share-book/{bookId}")
	public ResponseEntity<ApiResponseWithData<PostDTO>> shareBookAsPost(@PathVariable UUID bookId,
			@RequestBody PostDTO postDTO,
			@RequestHeader("Authorization") String jwt) {

		User currentUser = getCurrentUser(jwt);
		ensureUserCanPublish(currentUser);
		PostDTO post = postService.createBookSharePost(bookId, currentUser, postDTO);

		return buildSuccessResponse("Book shared successfully.", post);
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
	}

}