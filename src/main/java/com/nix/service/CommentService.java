package com.nix.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.nix.models.Comment;
import com.nix.models.User;

public interface CommentService {
	public List<Comment> getAllComments();

	public List<Comment> getAllParentComments();

	public Page<Comment> getPagerBookComments(int page, int size, UUID bookId);

	public Page<Comment> getPagerChapterComments(int page, int size, UUID chapterId);

	public Page<Comment> getPagerPostComments(int page, int size, UUID postId);
	
	public Page<Comment> getRecentCommentsByUserId(UUID userId, int page, int size);

	public Comment createPostComment(Comment comment, UUID postId, User user) throws Exception;

	public Comment createBookComment(Comment comment, UUID bookId, User user) throws Exception;

	public Comment createChapterComment(Comment comment, UUID chapterId, User user) throws Exception;

	public Boolean likeComment(UUID commentId, UUID userId) throws Exception;
	
	public Boolean isCommentLikedByCurrentUser(UUID commentId, User user);

	public Comment findCommentById(UUID commentId) throws Exception;

	public String deleteComment(UUID commentId, UUID userId) throws Exception;

	public Comment editComment(UUID userId, UUID commentId, Comment comment) throws Exception;

	public Comment createReplyBookComment(Comment comment, UUID parentCommentId, User user) throws Exception;

	public Comment createReplyChapterComment(Comment comment, UUID parentCommentId, User user) throws Exception;

	public Comment createReplyPostComment(Comment comment, UUID parentCommentId, User user) throws Exception;
}
