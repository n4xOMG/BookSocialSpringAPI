package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nix.dtos.PostDTO;
import com.nix.models.Post;
import com.nix.models.User;

@Component
public class PostMapper implements Mapper<Post, PostDTO> {
	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	@Autowired
	BookMapper bookMapper;

	ChapterSummaryMapper chapterSummaryMapper = new ChapterSummaryMapper();

	@Override
	public PostDTO mapToDTO(Post p) {
		PostDTO postDTO = new PostDTO();
		if (p.getId() != null) {
			postDTO.setId(p.getId());
		}
		postDTO.setImages(mapToImageDTOs(p.getImages()));
		postDTO.setContent(p.getContent());
		postDTO.setCommentCount(p.getComments().size());
		postDTO.setLikes(p.getLikes());
		postDTO.setTimestamp(p.getTimestamp());
		postDTO.setUser(userSummaryMapper.mapToDTO(p.getUser()));
		postDTO.setLikedByCurrentUser(false); // Default to false

		// Handle shared post
		if (p.getSharedPost() != null) {
			postDTO.setSharedPostId(p.getSharedPost().getId());
			postDTO.setSharedPostUser(userSummaryMapper.mapToDTO(p.getSharedPost().getUser()));
			postDTO.setSharedPostContent(p.getSharedPost().getContent());
			postDTO.setSharedPostImages(mapToImageDTOs(p.getSharedPost().getImages()));
			postDTO.setSharedPostTimestamp(p.getSharedPost().getTimestamp());
		}

		// Handle shared book
		if (p.getSharedBook() != null) {
			postDTO.setSharedBook(bookMapper.mapToDTO(p.getSharedBook()));
		}

		// Handle shared chapter
		if (p.getSharedChapter() != null) {
			postDTO.setSharedChapter(chapterSummaryMapper.mapToDTO(p.getSharedChapter()));
		}

		postDTO.setPostType(
				p.getPostType() != null ? PostDTO.PostType.valueOf(p.getPostType().name()) : PostDTO.PostType.STANDARD);

		return postDTO;
	}

	@Override
	public List<PostDTO> mapToDTOs(List<Post> posts) {
		return posts.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	public PostDTO mapToDTO(Post p, User currentUser) {
		PostDTO postDTO = mapToDTO(p); // Reuse existing method

		// Set liked status only if we have a current user
		if (currentUser != null) {
			postDTO.setLikedByCurrentUser(p.getLikedUsers().contains(currentUser));
		}

		return postDTO;
	}

	public List<PostDTO> mapToDTOs(List<Post> posts, User currentUser) {
		return posts.stream().map(post -> mapToDTO(post, currentUser)).collect(Collectors.toList());
	}

	private List<com.nix.dtos.ImageAttachmentDTO> mapToImageDTOs(List<com.nix.models.ImageAttachment> images) {
		if (images == null) {
			return new ArrayList<>();
		}
		return images.stream().map(img -> new com.nix.dtos.ImageAttachmentDTO(img.getUrl(), img.getIsMild()))
				.collect(Collectors.toList());
	}
}