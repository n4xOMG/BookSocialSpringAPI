package com.nix.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.PostDTO;
import com.nix.models.User;

public interface PostService {
	// Primary methods with user context
	Page<PostDTO> getAllPosts(Pageable pageable, User currentUser);

	List<PostDTO> getPostsByUser(User user, User currentUser);

	PostDTO getPostById(UUID postId, User currentUser);

	// Convenience methods without user context (they call the primary methods)
	Page<PostDTO> getAllPosts(Pageable pageable);

	List<PostDTO> getPostsByUser(User user);

	PostDTO getPostById(UUID postId);

	// Methods that always have a user context
	PostDTO createPost(User user, PostDTO postDTO);

	PostDTO updatePost(User user, UUID postId, PostDTO postDetails);

	void deletePost(User user, UUID postId);

	PostDTO likePost(UUID postId, User user);

	PostDTO createChapterSharePost(UUID chapterId, User user, PostDTO postDTO);

	PostDTO createBookSharePost(UUID bookId, User user, PostDTO postDTO);

	boolean isPostLikedByCurrentUser(User user, UUID postId);
}