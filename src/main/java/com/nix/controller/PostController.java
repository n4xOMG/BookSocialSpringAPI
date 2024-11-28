package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.PostDTO;
import com.nix.dtos.mappers.PostMapper;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Post;
import com.nix.models.User;
import com.nix.service.PostService;
import com.nix.service.UserService;

@RestController
public class PostController {
	@Autowired
	private PostService postService;

	@Autowired
	private UserService userService;

	PostMapper postMapper = new PostMapper();

	@GetMapping("/posts")
	public ResponseEntity<List<PostDTO>> getAllPosts() {
		List<Post> posts = postService.getAllPosts();
		return ResponseEntity.ok(postMapper.mapToDTOs(posts));
	}

	@GetMapping("/posts/{userId}")
	public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable("userId") Integer userId) {
		User user = userService.findUserById(userId);
		if (user == null) {
			throw new ResourceNotFoundException("Cannot find user with id: " + userId);
		}
		List<Post> posts = postService.getPostsByUser(user);
		return ResponseEntity.ok(postMapper.mapToDTOs(posts));
	}

	/**
	 * Create a new post
	 */
	@PostMapping("/api/posts")
	public ResponseEntity<PostDTO> createPost(@RequestBody Post post, @RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		Post createdPost = postService.createPost(user, post);
		return ResponseEntity.ok(postMapper.mapToDTO(createdPost));
	}

	/**
	 * Update a post
	 */
	@PutMapping("/api/posts/{id}")
	public ResponseEntity<PostDTO> updatePost(@PathVariable Integer id, @RequestBody Post postDetails,
			@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		Post updatedPost = postService.updatePost(user, id, postDetails);
		return ResponseEntity.ok(postMapper.mapToDTO(updatedPost));
	}

	/**
	 * Delete a post
	 */
	@DeleteMapping("/api/posts/{id}")
	public ResponseEntity<Void> deletePost(@PathVariable Integer id, @RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		postService.deletePost(user, id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Like a post
	 */
	@PostMapping("/api/posts/{postId}/like")
	public ResponseEntity<PostDTO> likePost(@PathVariable Integer postId, @RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user==null) {
			throw new ResourceNotFoundException("Cannot find user");
		}
		Post likedPost = postService.likePost(postId, user);
		return ResponseEntity.ok(postMapper.mapToDTO(likedPost));
	}

}
