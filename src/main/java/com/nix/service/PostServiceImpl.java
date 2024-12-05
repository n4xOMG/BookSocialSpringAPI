package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.PostDTO;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Post;
import com.nix.models.User;
import com.nix.repository.PostRepository;
import com.nix.repository.UserRepository;

@Service
public class PostServiceImpl implements PostService {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public List<Post> getAllPosts() {
		return postRepository.findAllByOrderByTimestampDesc();
	}

	@Override
	public List<Post> getPostsByUser(User user) {
		return postRepository.findByUser(user);
	}

	@Override
	public Post createPost(User user, PostDTO post) {
		Post newPost = new Post();
		newPost.setUser(user);
		newPost.setContent(post.getContent());
		if (post.getImages() != null) {
			newPost.setImages(post.getImages());
		}
		newPost.setLikes(0);
		newPost.setTimestamp(LocalDateTime.now());
		if (post.getSharedPostId() != null) {
			Post sharedPost = postRepository.findById(post.getSharedPostId()).orElseThrow(
					() -> new ResourceNotFoundException("Cannot find shared post with id: " + post.getSharedPostId()));
			newPost.setSharedPost(sharedPost);
		}
		return postRepository.save(newPost);
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
	@Transactional
	public void deletePost(User user, Integer postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to delete this post.");
		}
		for (User likedUser : post.getLikedUsers()) {
			likedUser.getLikedPosts().remove(post);
			userRepository.save(user);
		}
		postRepository.delete(post);
	}

	@Override
	public Post likePost(Integer postId, User user) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
		if (post.getLikedUsers().contains(user)) {
			post.getLikedUsers().remove(user);
			post.setLikes(post.getLikes() - 1);
		} else {
			post.getLikedUsers().add(user);
			post.setLikes(post.getLikes() + 1);
		}

		return postRepository.save(post);
	}

	@Override
	public Post getPostById(Integer postId) {
		return postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));
	}

}
