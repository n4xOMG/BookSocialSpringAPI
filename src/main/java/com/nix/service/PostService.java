package com.nix.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.PostDTO;
import com.nix.models.User;

public interface PostService {
	Page<PostDTO> getAllPosts(Pageable pageable);

	List<PostDTO> getPostsByUser(User user);

	PostDTO createPost(User user, PostDTO post);

	PostDTO updatePost(User user, Integer postId, PostDTO postDetails);

	void deletePost(User user, Integer postId);

	PostDTO likePost(Integer postId, User user);

	PostDTO getPostById(Integer postId);
}
