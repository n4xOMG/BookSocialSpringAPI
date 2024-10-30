package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.CommentDTO;
import com.nix.models.Comment;

public class CommentMapper implements Mapper<Comment, CommentDTO> {

	UserSummaryMapper userMapper = new UserSummaryMapper();

	@Override
	public CommentDTO mapToDTO(Comment comment) {

		CommentDTO commentDTO = new CommentDTO();
		commentDTO.setId(comment.getId());
		commentDTO.setContent(comment.getContent());

		commentDTO.setUser(userMapper.mapToDTO(comment.getUser()));
		commentDTO.setBookId(comment.getBook().getId());
		if (comment.getChapter() != null) {
			commentDTO.setChapterId(comment.getChapter().getId());
		}

		if (comment.getLikedUsers() != null) {
			commentDTO.setLikedUsers(comment.getLikedUsers().size());
		}
		if (comment.getReplies() != null) {
			commentDTO.setReplyComment(mapToDTOs(comment.getReplies()));
		}
		commentDTO.setCreatedAt(comment.getCreatedAt());
		return commentDTO;
	}

	@Override
	public List<CommentDTO> mapToDTOs(List<Comment> comments) {
		return comments.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
