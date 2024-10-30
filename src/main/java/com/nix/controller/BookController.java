package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.BookDTO;
import com.nix.dtos.mappers.BookMapper;
import com.nix.dtos.mappers.RatingMapper;
import com.nix.models.Book;
import com.nix.models.User;
import com.nix.response.ApiResponse;
import com.nix.service.BookService;
import com.nix.service.RatingService;
import com.nix.service.UserService;

@RestController
public class BookController {

	@Autowired
	BookService bookService;

	@Autowired
	UserService userService;

	@Autowired
	RatingService ratingService;

	BookMapper bookMapper = new BookMapper();

	RatingMapper ratingMapper = new RatingMapper();

	@Cacheable("books")
	@GetMapping("/books")
	public ResponseEntity<List<BookDTO>> getAllBooks() {
		List<BookDTO> books = bookMapper.mapToDTOs(bookService.getAllBooks());

		return new ResponseEntity<List<BookDTO>>(books, HttpStatus.ACCEPTED);
	}

	@GetMapping("/books/{bookId}")
	public ResponseEntity<BookDTO> getBookById(@PathVariable("bookId") Integer bookId) throws Exception {
		BookDTO book = bookMapper.mapToDTO(bookService.findBookById(bookId));

		if (book == null) {
			throw new Exception("Book not found");
		}

		return new ResponseEntity<>(book, HttpStatus.OK);
	}

	@GetMapping("/books/search")
	public ResponseEntity<List<BookDTO>> searchBooksByTitle(@RequestParam String query) {
		List<BookDTO> books = bookMapper.mapToDTOs(bookService.findBookByTitle(query));

		return new ResponseEntity<List<BookDTO>>(books, HttpStatus.ACCEPTED);
	}

	@GetMapping("/api/books/favoured")
	public ResponseEntity<List<BookDTO>> getUserFavouredBooks(@RequestHeader("Authorization") String jwt)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("No user found");
		}
		List<BookDTO> books = bookMapper.mapToDTOs(bookService.findBooksFollowedByUser(user.getId()));

		return new ResponseEntity<List<BookDTO>>(books, HttpStatus.ACCEPTED);
	}

	@GetMapping("/books/latest-update")
	public ResponseEntity<?> getBookWithLatestChapter() {
		Book book = bookService.getBookWithLatestChapter();
		return book != null ? ResponseEntity.ok(bookMapper.mapToDTO(book)) : ResponseEntity.notFound().build();
	}

	@CacheEvict(value = "books", allEntries = true)
	@PostMapping("/translator/books")
	public ResponseEntity<Book> addNewBook(@RequestHeader("Authorization") String jwt, @RequestBody Book book)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("No user found");
		}
		try {
			Book newBook = bookService.addNewBook(book);
			System.out.println("User " + user.getUsername() + " added new book " + newBook.getTitle());
			return new ResponseEntity<>(newBook, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@CachePut(value = "books", key = "#bookId")
	@PutMapping("/translator/books/{bookId}")
	public ResponseEntity<BookDTO> editBook(@RequestHeader("Authorization") String jwt,
			@PathVariable("bookId") Integer bookId, @RequestBody Book book) throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("No user found");
		}
		try {
			BookDTO editedBook = bookMapper.mapToDTO(bookService.updateBook(book, bookId));
			System.out.println("User " + user.getUsername() + " edited book " + editedBook.getTitle());
			return new ResponseEntity<>(editedBook, HttpStatus.ACCEPTED);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@CacheEvict(value = "books", key = "#bookId")
	@DeleteMapping("/translator/books/{bookId}")
	public ResponseEntity<ApiResponse> deleteBook(@PathVariable("bookId") Integer bookId) throws Exception {
		Book book = bookService.findBookById(bookId);
		if (book == null) {
			throw new Exception("Book not found");
		}
		ApiResponse res = new ApiResponse(bookService.deleteBook(bookId), true);
		return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
	}

	@PutMapping("/api/books/follow/{bookId}")
	public ResponseEntity<BookDTO> markBookAsFavoured(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer bookId) throws Exception {
		User reqUser = userService.findUserByJwt(jwt);
		if (reqUser != null) {
			BookDTO followedBook = bookMapper
					.mapToDTO(bookService.markAsFavouriteBook(bookService.findBookById(bookId), reqUser));
			return new ResponseEntity<>(followedBook, HttpStatus.ACCEPTED);
		}

		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}

}
