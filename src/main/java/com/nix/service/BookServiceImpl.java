package com.nix.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.Tag;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.CategoryRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.TagRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class BookServiceImpl implements BookService {

	@Autowired
	BookRepository bookRepo;

	@Autowired
	CategoryRepository categoryRepo;

	@Autowired
	TagRepository tagRepo;

	@Autowired
	ChapterRepository chaptereRepo;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Book findBookById(Integer bookId) throws Exception {
		Optional<Book> book = bookRepo.findById(bookId);
		if (book != null) {
			return book.get();
		}
		throw new Exception("No book found with id: " + bookId);
	}

	@Override
	public List<Book> findBooksByCategories(Integer categoryId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Book> findBookByTags(Integer tagId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Book> findRecentBooks(LocalDateTime starTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Book> findTopBooksByViews() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Book> findTopBooksByLikes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Book> findEditorSuggestedBooks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Book> findMostPurchaseBooks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Book> searchBooks(String query, Integer categoryId, Integer tagId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Book getBookWithLatestChapter() {
		return chaptereRepo.findTopByOrderByUploadDateDesc();
	}

	@Override
	public List<Book> findBookByTitle(String bookTitle) {
		List<Book> Books = bookRepo.findByTitle(bookTitle);

		return Books;
	}

	@Override
	public List<Book> findBooksByAuthor(Integer authorId) {
		List<Book> Books = bookRepo.findBooksByAuthor(authorId);

		return Books;
	}

	@Override
	public List<Book> getAllBooks() {
		return bookRepo.findAll();
	}

	@Override
	@Transactional
	public Book addNewBook(Book book) {
		Book newBook = new Book();
		newBook.setTitle(book.getTitle());
		newBook.setAuthorName(book.getAuthorName());
		newBook.setArtistName(book.getArtistName());
		newBook.setBookCover(book.getBookCover());
		newBook.setDescription(book.getDescription());
		newBook.setUploadDate(LocalDateTime.now());
		newBook.setLanguage(book.getLanguage());
		newBook.setSuggested(false);
		newBook.setViewCount(0);

		if (book.getCategories() != null) {
			List<Category> categories = new ArrayList<>();
			for (Category category : book.getCategories()) {
				Category managedCategory = categoryRepo.findById(category.getId())
						.orElseThrow(() -> new RuntimeException("Cannot find category"));
				managedCategory.getBooks().add(newBook);
				categories.add(managedCategory);
			}
			newBook.setCategories(categories);
		}

		if (book.getTags() != null) {
			List<Tag> tags = new ArrayList<>();
			for (Tag tag : book.getTags()) {
				Tag managedTag = tagRepo.findById(tag.getId())
						.orElseThrow(() -> new RuntimeException("Cannot find category"));
				managedTag.getBooks().add(newBook);
				tags.add(managedTag);
			}
			newBook.setTags(tags);
		}

		return bookRepo.save(newBook);
	}

	@Override
	@Transactional
	public Book updateBook(Book book, Integer bookId) throws Exception {
		try {
			Book updateBook = findBookById(bookId);

			if (book.getTitle() != null) {
				updateBook.setTitle(book.getTitle());
			}

			if (book.getBookCover() != null) {
				updateBook.setBookCover(book.getBookCover());
			}

			if (book.getAuthorName() != null) {
				updateBook.setAuthorName(book.getAuthorName());
			}
			if (book.getArtistName() != null) {
				updateBook.setArtistName(book.getArtistName());
			}
			if (book.getDescription() != null) {
				updateBook.setDescription(book.getDescription());
			}

			if (book.getCategories() != null) {
				List<Category> newCategories = new ArrayList<>();
				for (Category category : book.getCategories()) {
					category.getBooks().add(updateBook);
					newCategories.add(category);
				}
				updateBook.setCategories(newCategories);
			}

			if (book.getTags() != null) {
				List<Tag> newTags = new ArrayList<>();
				for (Tag tag : book.getTags()) {
					tag.getBooks().add(updateBook);
					newTags.add(tag);
				}
				updateBook.setTags(newTags);
			}

			bookRepo.save(updateBook);

			return updateBook;
		} catch (Exception e) {
			throw new Exception("Error updating book " + e);
		}
	}

	@Override
	@Transactional
	public String deleteBook(Integer bookId) throws Exception {
		Book deleteBook = findBookById(bookId);
		try {
			if (deleteBook != null) {
				deleteBook.getFavoured().forEach(user -> user.getFollowedBooks().remove(deleteBook));
				deleteBook.getFavoured().clear();

				deleteBook.setAuthor(null);
				// Remove categories properly
				deleteBook.getCategories().forEach(category -> {
					category.getBooks().remove(deleteBook);
					entityManager.remove(category);
				});
				deleteBook.getCategories().clear();

				deleteBook.getTags().forEach(tag -> {
					tag.getBooks().remove(deleteBook);
					entityManager.remove(tag);
				});

				// Remove chapters
				deleteBook.getChapters().forEach(chapter -> entityManager.remove(chapter));
				deleteBook.getChapters().clear();

				entityManager.flush();

				// Remove the book
				entityManager.remove(deleteBook);

				return "Book deleted successfully!";
			} else {
				return "Book not found!";
			}
		} catch (Exception e) {
			System.err.println("Error deleting book: " + e.getMessage());
			throw new Exception("Error deleting book: " + e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public Book markAsFavouriteBook(Book book, User user) {

		if (book.getFavoured().contains(user)) {
			book.getFavoured().remove(user);
			user.getFollowedBooks().remove(book);
		} else {
			book.getFavoured().add(user);
			user.getFollowedBooks().add(book);
		}

		return book;
	}

	@Override
	public List<Book> findBooksFollowedByUser(Integer userId) {
		return bookRepo.findByUserFavoured(userId);
	}

	@Override
	public void incrementBookViewCount(Integer bookId) {
		bookRepo.incrementViewCount(bookId);

	}

}
