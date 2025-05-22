package com.nix.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.PostDTO;
import com.nix.models.User;

public interface PostService {
	// Primary methods with user context
	Page<PostDTO> getAllPosts(Pageable pageable, User currentUser);

	List<PostDTO> getPostsByUser(User user, User currentUser);

	PostDTO getPostById(Long postId, User currentUser);

	// Convenience methods without user context (they call the primary methods)
	Page<PostDTO> getAllPosts(Pageable pageable);

	List<PostDTO> getPostsByUser(User user);

	PostDTO getPostById(Long postId);

	// Methods that always have a user context
	PostDTO createPost(User user, PostDTO postDTO);

	PostDTO updatePost(User user, Long postId, PostDTO postDetails);

	void deletePost(User user, Long postId);

	PostDTO likePost(Long postId, User user);

	PostDTO createChapterSharePost(Long chapterId, User user, PostDTO postDTO);

	PostDTO createBookSharePost(Long bookId, User user, PostDTO postDTO);

	boolean isPostLikedByCurrentUser(User user, Long postId);
}