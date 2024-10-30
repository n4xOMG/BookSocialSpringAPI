package com.nix.service;

import java.util.List;

import com.nix.models.Comment;

public interface CommentService {
	public List<Comment> getAllComments();
	
	public List<Comment> getAllParentComments();
	
	public List<Comment> getAllBookComments(Integer bookId);
	
	public List<Comment> getAllChapterComments(Integer chapterId);

	public Comment createBookComment(Comment comment, Integer bookId, Integer userId) throws Exception;

	public Comment createChapterComment(Comment comment, Integer bookId, Integer chapterId, Integer userId) throws Exception;

	public Comment likeComment(Integer commentId, Integer userId) throws Exception;

	public Comment findCommentById(Integer commentId) throws Exception;
	
	public String deleteComment(Integer commentId, Integer userId) throws Exception;
	
	public Comment editComment(Integer userId, Integer commentId, Comment comment) throws Exception;
	
	public Comment createReplyBookComment(Comment comment, Integer parentCommentId, Integer userId) throws Exception;
	public Comment createReplyChapterComment(Comment comment, Integer parentCommentId, Integer userId) throws Exception;
}
