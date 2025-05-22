package com.nix.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.nix.models.Comment;
import com.nix.models.User;

public interface CommentService {
	public List<Comment> getAllComments();

	public List<Comment> getAllParentComments();

	public Page<Comment> getPagerBookComments(int page, int size, Long bookId);

	public Page<Comment> getPagerChapterComments(int page, int size, Long chapterId);

	public Page<Comment> getPagerPostComments(int page, int size, Long postId);
	
	public Page<Comment> getRecentCommentsByUserId(Long userId, int page, int size);

	public Comment createPostComment(Comment comment, Long postId, User user) throws Exception;

	public Comment createBookComment(Comment comment, Long bookId, User user) throws Exception;

	public Comment createChapterComment(Comment comment, Long chapterId, User user) throws Exception;

	public Boolean likeComment(Long commentId, Long userId) throws Exception;
	
	public Boolean isCommentLikedByCurrentUser(Long commentId, User user);

	public Comment findCommentById(Long commentId) throws Exception;

	public String deleteComment(Long commentId, Long userId) throws Exception;

	public Comment editComment(Long userId, Long commentId, Comment comment) throws Exception;

	public Comment createReplyBookComment(Comment comment, Long parentCommentId, User user) throws Exception;

	public Comment createReplyChapterComment(Comment comment, Long parentCommentId, User user) throws Exception;

	public Comment createReplyPostComment(Comment comment, Long parentCommentId, User user) throws Exception;
}
