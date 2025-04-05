package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Sort;
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
import com.nix.models.User;
import com.nix.service.PostService;
import com.nix.service.UserService;

@RestController
public class PostController {
	@Autowired
	private PostService postService;

	@Autowired
	private UserService userService;

	@GetMapping("/posts")
	public ResponseEntity<Page<PostDTO>> getAllPosts(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "timestamp,desc") String sort) {
		Pageable pageable = PageRequest.of(page, size, parseSort(sort));
		Page<PostDTO> postsPage = postService.getAllPosts(pageable);
		return ResponseEntity.ok(postsPage);
	}

	// Helper method to parse sort parameter
	private Sort parseSort(String sort) {
		String[] parts = sort.split(",");
		String property = parts[0];
		Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC
				: Sort.Direction.DESC;
		return Sort.by(direction, property);
	}

	@GetMapping("/posts/{postId}")
	public ResponseEntity<PostDTO> getPostById(@PathVariable Integer postId) {
		PostDTO post = postService.getPostById(postId);
		return ResponseEntity.ok(post);
	}

	@GetMapping("/posts/user/{userId}")
	public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable("userId") Integer userId) {
		User user = userService.findUserById(userId);
		if (user == null) {
			throw new ResourceNotFoundException("Cannot find user with id: " + userId);
		}
		List<PostDTO> posts = postService.getPostsByUser(user);
		return ResponseEntity.ok(posts);
	}

	@PostMapping("/api/posts")
	public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO,
			@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		PostDTO createdPost = postService.createPost(user, postDTO);
		return ResponseEntity.ok(createdPost);
	}

	@PutMapping("/api/posts/{id}")
	public ResponseEntity<PostDTO> updatePost(@PathVariable Integer id, @RequestBody PostDTO postDetails,
			@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		PostDTO updatedPost = postService.updatePost(user, id, postDetails);
		return ResponseEntity.ok(updatedPost);
	}

	@DeleteMapping("/api/posts/{id}")
	public ResponseEntity<Void> deletePost(@PathVariable Integer id, @RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		postService.deletePost(user, id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/posts/{postId}/like")
	public ResponseEntity<PostDTO> likePost(@PathVariable Integer postId, @RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new ResourceNotFoundException("Cannot find user");
		}
		PostDTO likedPost = postService.likePost(postId, user);
		return ResponseEntity.ok(likedPost);
	}
}