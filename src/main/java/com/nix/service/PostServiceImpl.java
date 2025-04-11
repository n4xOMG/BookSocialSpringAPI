package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.PostDTO;
import com.nix.dtos.mappers.PostMapper;
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

	@Autowired
	private PostMapper postMapper;

	@Override
	public Page<PostDTO> getAllPosts(Pageable pageable, User currentUser) {
		Page<Post> postsPage = postRepository.findAll(pageable);
		if (currentUser != null) {
			return postsPage.map(post -> postMapper.mapToDTO(post, currentUser));
		} else {
			return postsPage.map(postMapper::mapToDTO);
		}
	}

	@Override
	public Page<PostDTO> getAllPosts(Pageable pageable) {
		return getAllPosts(pageable, null);
	}

	@Override
	public List<PostDTO> getPostsByUser(User user, User currentUser) {
		List<Post> posts = postRepository.findByUser(user);
		if (currentUser != null) {
			return postMapper.mapToDTOs(posts, currentUser);
		} else {
			return postMapper.mapToDTOs(posts);
		}
	}

	@Override
	public List<PostDTO> getPostsByUser(User user) {
		return getPostsByUser(user, null);
	}

	@Override
	public PostDTO getPostById(Integer postId, User currentUser) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (currentUser != null) {
			return postMapper.mapToDTO(post, currentUser);
		} else {
			return postMapper.mapToDTO(post);
		}
	}

	@Override
	public PostDTO getPostById(Integer postId) {
		return getPostById(postId, null);
	}

	@Override
	public PostDTO createPost(User user, PostDTO postDTO) {
		Post newPost = new Post();
		newPost.setUser(user);
		newPost.setContent(postDTO.getContent());
		if (postDTO.getImages() != null) {
			newPost.setImages(postDTO.getImages());
		}
		newPost.setLikes(0);
		newPost.setTimestamp(LocalDateTime.now());
		if (postDTO.getSharedPostId() != null) {
			Post sharedPost = postRepository.findById(postDTO.getSharedPostId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Cannot find shared post with id: " + postDTO.getSharedPostId()));
			newPost.setSharedPost(sharedPost);
		}

		Post savedPost = postRepository.save(newPost);
		return postMapper.mapToDTO(savedPost, user);
	}

	@Override
	public PostDTO updatePost(User user, Integer postId, PostDTO postDetails) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to update this post.");
		}

		post.setContent(postDetails.getContent());
		post.setImages(postDetails.getImages());

		Post savedPost = postRepository.save(post);
		return postMapper.mapToDTO(savedPost, user);
	}

	@Override
	@Transactional
	public void deletePost(User user, Integer postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		if (!post.getUser().getId().equals(user.getId())) {
			throw new ResourceNotFoundException("User is not authorized to delete this post.");
		}

		// Clean up liked users references
		for (User likedUser : post.getLikedUsers()) {
			likedUser.getLikedPosts().remove(post);
			userRepository.save(likedUser);
		}

		postRepository.delete(post);
	}

	@Override
	public PostDTO likePost(Integer postId, User user) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

		boolean wasLiked = post.getLikedUsers().contains(user);

		if (wasLiked) {
			post.getLikedUsers().remove(user);
			post.setLikes(post.getLikes() - 1);
		} else {
			post.getLikedUsers().add(user);
			post.setLikes(post.getLikes() + 1);
		}

		Post savedPost = postRepository.save(post);
		return postMapper.mapToDTO(savedPost, user);
	}
}