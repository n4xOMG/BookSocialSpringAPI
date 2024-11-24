package com.nix.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Post;
import com.nix.models.User;
import com.nix.repository.PostRepository;

@Service
public class PostServiceImpl implements PostService {

	@Autowired
	private PostRepository postRepository;

	@Override
	public List<Post> getAllPosts() {
		return postRepository.findAllByOrderByTimestampDesc();
	}

	@Override
	public Post createPost(User user, Post post) {
		post.setUser(user);
		post.setTimestamp(java.time.LocalDateTime.now());
		return postRepository.save(post);
	}

	@Override
	public Post updatePost(User user, Integer postId, Post postDetails) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to update this post.");
		}

		post.setContent(postDetails.getContent());
		post.setImages(postDetails.getImages());
		return postRepository.save(post);
	}

	@Override
	public void deletePost(User user, Integer postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to delete this post.");
		}

		postRepository.delete(post);
	}

	@Override
	public Post likePost(Integer postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		post.setLikes(post.getLikes() + 1);
		return postRepository.save(post);
	}

}
