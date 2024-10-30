package com.nix.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.exception.SensitiveWordException;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Comment;
import com.nix.models.SensitiveWord;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.CommentRepository;
import com.nix.repository.SensitiveWordRepository;
import com.nix.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CommentServiceImpl implements CommentService {

	@Autowired
	CommentRepository commentRepo;

	@Autowired
	BookRepository bookRepo;

	@Autowired
	UserRepository userRepo;

	@Autowired
	ChapterRepository chapterRepo;

	@Autowired
	SensitiveWordRepository sensitiveWordRepo;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Comment> getAllComments() {
		return commentRepo.findAll();
	}

	@Override
	public List<Comment> getAllParentComments() {
		return commentRepo.findParentComments();
	}

	@Override
	public List<Comment> getAllBookComments(Integer bookId) {
		List<Comment> comments = commentRepo.findParentCommentsByBookId(bookId);
		return comments;

	}

	@Override
	public List<Comment> getAllChapterComments(Integer chapterId) {
		List<Comment> comments = commentRepo.findParentCommentsByChapterId(chapterId);
		return comments;
	}

	public boolean containsSensitiveWords(String content) {
		String normalizedContent = normalizeContent(content);

		List<SensitiveWord> sensitiveWords = sensitiveWordRepo.findAll();

		return sensitiveWords.stream().anyMatch(word -> {
			String normalizedWord = word.getWord().toLowerCase();

			// Use Pattern and Matcher to check for whole words, ignoring newlines and extra
			// spaces
			String wordPattern = "\\b" + Pattern.quote(normalizedWord) + "\\b";
			Pattern pattern = Pattern.compile(wordPattern, Pattern.DOTALL); // Use DOTALL to match across newlines
			Matcher matcher = pattern.matcher(normalizedContent);
			return matcher.find();
		});
	}

	// Helper method to normalize the content
	private String normalizeContent(String content) {
		String normalizedContent = normalizeLeetSpeak(content.toLowerCase());

		// Replace newlines, tabs, and multiple spaces with a single space
		normalizedContent = normalizedContent.replaceAll("[\\n\\r\\t]+", " ");
		normalizedContent = normalizedContent.replaceAll("\\s{2,}", " ");

		// Remove non-alphabetic characters (but retain spaces)
		return normalizedContent.replaceAll("[^a-z\\s]", "");
	}

	// Helper function to normalize leetspeak and symbol substitutions
	private String normalizeLeetSpeak(String content) {
		Map<Character, Character> leetSpeakMap = getLeetSpeakMap();

		// Replace leetspeak characters in the content
		StringBuilder normalizedContent = new StringBuilder();
		for (char c : content.toCharArray()) {
			// Replace character if it's in the leetspeak map, otherwise keep it as is
			normalizedContent.append(leetSpeakMap.getOrDefault(c, c));
		}
		return normalizedContent.toString();
	}

	// Define a leetspeak map for common letter substitutions
	private Map<Character, Character> getLeetSpeakMap() {
		Map<Character, Character> leetSpeakMap = new HashMap<>();
		leetSpeakMap.put('1', 'i');
		leetSpeakMap.put('!', 'i');
		leetSpeakMap.put('@', 'a');
		leetSpeakMap.put('3', 'e');
		leetSpeakMap.put('4', 'a');
		leetSpeakMap.put('0', 'o');
		leetSpeakMap.put('7', 't');
		leetSpeakMap.put('$', 's');
		return leetSpeakMap;
	}

	@Transactional
	public Comment createComment(Comment comment, Integer bookId, Integer chapterId, Integer userId,
			boolean isChapterComment) throws Exception {

		Optional<Book> book = bookRepo.findById(bookId);
		if (book == null) {
			throw new Exception("No book found!");
		}

		Optional<User> user = userRepo.findById(userId);
		if (user == null) {
			throw new Exception("User not found!");
		}
		if (containsSensitiveWords(comment.getContent())) {
			throw new SensitiveWordException("Comment contains sensitive words");
		}
		Comment newComment = new Comment();
		newComment.setUser(user.get());
		newComment.setBook(book.get());
		newComment.setContent(comment.getContent());
		newComment.setCreatedAt(LocalDateTime.now());
		if (isChapterComment) {
			Optional<Chapter> chapter = chapterRepo.findById(chapterId);
			if (chapter == null) {
				throw new Exception("No chapter found!");
			}
			newComment.setChapter(chapter.get());
			chapter.get().getComments().add(newComment);
		}
		book.get().getComments().add(newComment);

		return commentRepo.save(newComment);
	}

	@Override
	@Transactional
	public Comment createBookComment(Comment comment, Integer bookId, Integer userId) throws Exception {
		return createComment(comment, bookId, null, userId, false);
	}

	@Override
	@Transactional
	public Comment createChapterComment(Comment comment, Integer bookId, Integer chapterId, Integer userId)
			throws Exception {
		return createComment(comment, bookId, chapterId, userId, true);
	}

	@Override
	@Transactional
	public Comment likeComment(Integer commentId, Integer userId) throws Exception {

		Optional<User> user = userRepo.findById(userId);
		if (user == null) {
			throw new Exception("User not found!");
		}

		Comment comment = findCommentById(commentId);
		if (comment == null) {
			throw new Exception("Comment not found!");
		}

		if (comment.getLikedUsers().contains(user.get())) {
			comment.getLikedUsers().remove(user.get());
			user.get().getLikedComments().remove(comment);
		} else {
			comment.getLikedUsers().add(user.get());
			user.get().getLikedComments().add(comment);
		}

		return commentRepo.save(comment);
	}

	@Override
	public Comment findCommentById(Integer commentId) throws Exception {
		Optional<Comment> comment = commentRepo.findById(commentId);
		if (comment != null) {
			return comment.get();
		}
		throw new Exception("Cannot find comment with id: " + commentId);
	}

	@Override
	@Transactional
	public String deleteComment(Integer commentId, Integer userId) throws Exception {
	    Comment comment = findCommentById(commentId);
	    Optional<User> reqUser = userRepo.findById(userId);

	    if (!reqUser.isPresent() || 
	        (!reqUser.get().getRole().getName().equals("ADMIN") && 
	         !comment.getUser().getId().equals(userId))) {
	        return "Cannot delete comment, invalid user!";
	    }

	    try {
	        // Ensure the comment is managed
	        comment = entityManager.merge(comment);

	        // Delete all reply comments one by one
	        for (Comment reply : comment.getReplies()) {
	            reply = entityManager.merge(reply); // Ensure it's managed
	            entityManager.remove(reply); // Remove the reply
	        }
	        comment.getReplies().clear(); // Clear the replies list

	        // Remove associations with liked users
	        for (User user : comment.getLikedUsers()) {
	            user.getLikedComments().remove(comment);
	        }
	        comment.getLikedUsers().clear();

	        // Remove the parent comment
	        entityManager.remove(comment);

	        return "Comment deleted successfully!";
	    } catch (Exception e) {
	        System.err.println("Error deleting comment: " + e.getMessage());
	        throw new Exception("Error deleting comment: " + e.getMessage(), e);
	    }
	}


	@Override
	public Comment editComment(Integer userId, Integer commentId, Comment comment) throws Exception {
		Comment editComment = findCommentById(commentId);

		if (editComment.getUser().getId() != userId) {
			throw new Exception("Cannot delete comment, invaid user!");
		}

		if (comment.getContent() != null) {
			editComment.setContent(comment.getContent());
			editComment.setCreatedAt(LocalDateTime.now());
		}

		return commentRepo.save(editComment);
	}

	@Transactional
	public Comment createReplyComment(Comment comment, Integer parentCommentId, Integer userId,
			boolean isChapterComment) throws Exception {
		Comment parentComment = findCommentById(parentCommentId);

		if (parentComment == null) {
			throw new Exception("Parent comment not found!");
		}

		Optional<User> user = userRepo.findById(userId);
		if (user.isEmpty()) {
			throw new Exception("User not found!");
		}

		if (containsSensitiveWords(comment.getContent())) {
			throw new SensitiveWordException("Comment contains sensitive words");
		}

		Comment newComment = new Comment();
		newComment.setUser(user.get());
		newComment.setContent(comment.getContent());
		newComment.setCreatedAt(LocalDateTime.now());
		newComment.setParentComment(parentComment);
		newComment.setBook(parentComment.getBook());

		if (isChapterComment) {
			newComment.setChapter(parentComment.getChapter());
		}

		parentComment.getReplies().add(newComment);

		return commentRepo.save(newComment);
	}

	@Override
	@Transactional
	public Comment createReplyBookComment(Comment comment, Integer parentCommentId, Integer userId) throws Exception {
		return createReplyComment(comment, parentCommentId, userId, false);
	}

	@Override
	@Transactional
	public Comment createReplyChapterComment(Comment comment, Integer parentCommentId, Integer userId)
			throws Exception {
		return createReplyComment(comment, parentCommentId, userId, true);
	}
}
