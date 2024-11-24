package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.PostDTO;
import com.nix.models.Post;

public class PostMapper implements Mapper<Post, PostDTO> {
	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	@Override
	public PostDTO mapToDTO(Post p) {
		PostDTO postDTO = new PostDTO();
		if (p.getId() != null) {
			postDTO.setId(p.getId());
		}
		postDTO.setImages(p.getImages());
		postDTO.setContent(p.getContent());
		postDTO.setComments(p.getComments().size());
		postDTO.setLikes(p.getLikes());
		postDTO.setTimestamp(p.getTimestamp());
		postDTO.setUser(userSummaryMapper.mapToDTO(p.getUser()));

		return postDTO;
	}

	@Override
	public List<PostDTO> mapToDTOs(List<Post> posts) {
		return posts.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
