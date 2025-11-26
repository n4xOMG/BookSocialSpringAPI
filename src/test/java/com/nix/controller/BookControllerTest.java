package com.nix.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nix.dtos.BookDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.dtos.UserSummaryDTO;
import com.nix.models.Role;
import com.nix.models.User;
import com.nix.service.BookService;
import com.nix.service.NotificationService;
import com.nix.service.UserService;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @MockBean
    private UserService userService;

    @MockBean
    private NotificationService notificationService;

    @Test
    void getAllBooks_withoutAuth_returnsAllBooks() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookDTO bookDTO = buildBookDTO(bookId, "Test Book", null);
        Page<BookDTO> page = new PageImpl<>(Collections.singletonList(bookDTO));

        when(bookService.getAllBooks(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/books")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(bookId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Test Book"));

        verify(bookService).getAllBooks(any(Pageable.class));
    }

    @Test
    void getAllBooks_withAuth_filtersBlockedAuthors() throws Exception {
        String jwt = "Bearer test-token";
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        User currentUser = buildUser(userId, "USER");
        BookDTO bookDTO = buildBookDTO(bookId, "Test Book", null);
        Page<BookDTO> page = new PageImpl<>(Collections.singletonList(bookDTO));

        when(userService.findUserByJwt(jwt)).thenReturn(currentUser);
        when(userService.getUserIdsBlocking(userId)).thenReturn(Set.of());
        when(userService.getBlockedUserIds(userId)).thenReturn(Set.of());
        when(bookService.getAllBooks(any(Pageable.class), anySet())).thenReturn(page);
        when(bookService.getFavouriteBookIdsForUser(userId)).thenReturn(Set.of());

        mockMvc.perform(get("/books")
                .header("Authorization", jwt)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(bookId.toString()));

        verify(bookService).getAllBooks(any(Pageable.class), anySet());
    }

    @Test
    void getBookById_withoutAuth_returnsBook() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookDTO bookDTO = buildBookDTO(bookId, "Test Book", null);

        when(bookService.getBookById(bookId)).thenReturn(bookDTO);

        mockMvc.perform(get("/books/{bookId}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId.toString()))
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void getBookById_withAuth_setsFollowedStatus() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User currentUser = buildUser(userId, "USER");
        BookDTO bookDTO = buildBookDTO(bookId, "Test Book", null);

        when(userService.findUserByJwt(jwt)).thenReturn(currentUser);
        when(bookService.getBookById(bookId)).thenReturn(bookDTO);
        when(bookService.isBookLikedByUser(userId, bookId)).thenReturn(true);

        mockMvc.perform(get("/books/{bookId}", bookId)
                .header("Authorization", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId.toString()))
                .andExpect(jsonPath("$.followedByCurrentUser").value(true));
    }

    @Test
    void getBooksByAuthor_withValidAuthorId_returnsBooks() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BookDTO bookDTO = buildBookDTO(bookId, "Author's Book", authorId);
        Page<BookDTO> page = new PageImpl<>(Collections.singletonList(bookDTO));

        when(bookService.getBooksByAuthor(eq(authorId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/books/author/{authorId}", authorId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(bookId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Author's Book"));
    }

    @Test
    void searchBooks_withTitleAndCategory_returnsMatchingBooks() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookDTO bookDTO = buildBookDTO(bookId, "Fantasy Novel", null);
        Page<BookDTO> page = new PageImpl<>(Collections.singletonList(bookDTO));

        when(bookService.searchBooks(eq("Fantasy"), eq(1), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/books/search")
                .param("title", "Fantasy")
                .param("categoryId", "1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Fantasy Novel"));
    }

    @Test
    void getTop10BooksByLikes_returnsTopBooks() throws Exception {
        UUID bookId1 = UUID.randomUUID();
        UUID bookId2 = UUID.randomUUID();
        List<BookDTO> topBooks = Arrays.asList(
                buildBookDTO(bookId1, "Popular Book 1", null),
                buildBookDTO(bookId2, "Popular Book 2", null));

        when(bookService.getTop10LikedBooks()).thenReturn(topBooks);

        mockMvc.perform(get("/books/top-likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Popular Book 1"))
                .andExpect(jsonPath("$[1].title").value("Popular Book 2"));
    }

    @Test
    void getFeaturedBooks_returnsFeaturedBooks() throws Exception {
        UUID bookId = UUID.randomUUID();
        List<BookDTO> featuredBooks = Collections.singletonList(
                buildBookDTO(bookId, "Featured Book", null));

        when(bookService.getFeaturedBooks()).thenReturn(featuredBooks);

        mockMvc.perform(get("/books/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Featured Book"));
    }

    @Test
    void getTrendingBooks_withParams_returnsTrendingBooks() throws Exception {
        UUID bookId = UUID.randomUUID();
        List<BookDTO> trendingBooks = Collections.singletonList(
                buildBookDTO(bookId, "Trending Book", null));

        when(bookService.getTrendingBooks(24, 10L, 10)).thenReturn(trendingBooks);

        mockMvc.perform(get("/books/trending")
                .param("hours", "24")
                .param("minViews", "10")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Trending Book"));
    }

    @Test
    void getTopSixCategoriesWithBooks_returnsCategories() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId(1);
        category.setName("Fantasy");
        category.setBooks(Collections.singletonList(buildBookDTO(UUID.randomUUID(), "Fantasy Book", null)));

        when(bookService.getTopSixCategoriesWithBooks()).thenReturn(Collections.singletonList(category));

        mockMvc.perform(get("/top-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Fantasy"));
    }

    @Test
    void createBook_withVerifiedUser_createsBook() throws Exception {
        String jwt = "Bearer test-token";
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        User user = buildVerifiedUser(userId);
        BookDTO bookDTO = buildBookDTO(bookId, "New Book", userId);

        when(userService.findUserByJwt(jwt)).thenReturn(user);
        when(bookService.createBook(any(BookDTO.class), eq(userId))).thenReturn(bookDTO);
        when(userService.findUserById(userId)).thenReturn(user);

        mockMvc.perform(post("/api/books")
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Book created successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("New Book"));

        verify(bookService).createBook(any(BookDTO.class), eq(userId));
    }

    @Test
    void createBook_withBannedUser_returnsForbidden() throws Exception {
        String jwt = "Bearer test-token";
        UUID userId = UUID.randomUUID();
        User bannedUser = buildUser(userId, "USER");
        bannedUser.setBanned(true);
        BookDTO bookDTO = buildBookDTO(UUID.randomUUID(), "New Book", userId);

        when(userService.findUserByJwt(jwt)).thenReturn(bannedUser);

        mockMvc.perform(post("/api/books")
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You are currently banned from this website. Contact support for assistance."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createBook_withSuspendedUser_returnsForbidden() throws Exception {
        String jwt = "Bearer test-token";
        UUID userId = UUID.randomUUID();
        User suspendedUser = buildUser(userId, "USER");
        suspendedUser.setIsSuspended(true);
        BookDTO bookDTO = buildBookDTO(UUID.randomUUID(), "New Book", userId);

        when(userService.findUserByJwt(jwt)).thenReturn(suspendedUser);

        mockMvc.perform(post("/api/books")
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are currently suspended. Contact support for assistance."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createBook_withUnverifiedUser_returnsForbidden() throws Exception {
        String jwt = "Bearer test-token";
        UUID userId = UUID.randomUUID();
        User unverifiedUser = buildUser(userId, "USER");
        unverifiedUser.setIsVerified(false);
        BookDTO bookDTO = buildBookDTO(UUID.randomUUID(), "New Book", userId);

        when(userService.findUserByJwt(jwt)).thenReturn(unverifiedUser);

        mockMvc.perform(post("/api/books")
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please verify your account before creating books."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateBook_asAuthor_updatesBook() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        User author = buildVerifiedUser(authorId);
        BookDTO existingBook = buildBookDTO(bookId, "Old Title", authorId);
        BookDTO updatedBook = buildBookDTO(bookId, "New Title", authorId);

        when(userService.findUserByJwt(jwt)).thenReturn(author);
        when(bookService.getBookById(bookId)).thenReturn(existingBook);
        when(bookService.updateBook(eq(bookId), any(BookDTO.class))).thenReturn(updatedBook);
        when(userService.findUserById(authorId)).thenReturn(author);

        mockMvc.perform(put("/api/books/{bookId}", bookId)
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book updated successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("New Title"));

        verify(bookService).updateBook(eq(bookId), any(BookDTO.class));
    }

    @Test
    void updateBook_asAdmin_updatesBook() throws Exception {
        String jwt = "Bearer admin-token";
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User admin = buildUser(adminId, "ADMIN");
        admin.setIsVerified(true);
        BookDTO existingBook = buildBookDTO(bookId, "Old Title", authorId);
        BookDTO updatedBook = buildBookDTO(bookId, "New Title", authorId);

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(bookService.getBookById(bookId)).thenReturn(existingBook);
        when(bookService.updateBook(eq(bookId), any(BookDTO.class))).thenReturn(updatedBook);
        when(userService.findUserById(authorId)).thenReturn(buildVerifiedUser(authorId));

        mockMvc.perform(put("/api/books/{bookId}", bookId)
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateBook_asNonAuthor_returnsForbidden() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User otherUser = buildVerifiedUser(otherUserId);
        BookDTO existingBook = buildBookDTO(bookId, "Book Title", authorId);

        when(userService.findUserByJwt(jwt)).thenReturn(otherUser);
        when(bookService.getBookById(bookId)).thenReturn(existingBook);

        mockMvc.perform(put("/api/books/{bookId}", bookId)
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(existingBook)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to edit this book."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteBook_asAuthor_deletesBook() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        User author = buildVerifiedUser(authorId);
        BookDTO book = buildBookDTO(bookId, "Book to Delete", authorId);

        when(userService.findUserByJwt(jwt)).thenReturn(author);
        when(bookService.getBookById(bookId)).thenReturn(book);
        when(userService.findUserById(authorId)).thenReturn(author);

        mockMvc.perform(delete("/api/books/{bookId}", bookId)
                .header("Authorization", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book deleted successfully."))
                .andExpect(jsonPath("$.success").value(true));

        verify(bookService).deleteBook(bookId);
    }

    @Test
    void deleteBook_asNonAuthor_returnsForbidden() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User otherUser = buildVerifiedUser(otherUserId);
        BookDTO book = buildBookDTO(bookId, "Book Title", authorId);

        when(userService.findUserByJwt(jwt)).thenReturn(otherUser);
        when(bookService.getBookById(bookId)).thenReturn(book);

        mockMvc.perform(delete("/api/books/{bookId}", bookId)
                .header("Authorization", jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to delete this book."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void recordBookView_withAuth_recordsView() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = buildVerifiedUser(userId);
        BookDTO bookDTO = buildBookDTO(bookId, "Book Title", null);

        when(userService.findUserByJwt(jwt)).thenReturn(user);
        when(bookService.getBookById(bookId)).thenReturn(bookDTO);
        when(bookService.recordBookView(eq(bookId), eq(userId), anyString())).thenReturn(150L);

        mockMvc.perform(post("/books/{bookId}/views", bookId)
                .header("Authorization", jwt))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Book view recorded successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(150));

        verify(bookService).recordBookView(eq(bookId), eq(userId), anyString());
    }

    @Test
    void recordBookView_withoutAuth_recordsViewWithoutUserId() throws Exception {
        UUID bookId = UUID.randomUUID();

        when(bookService.recordBookView(eq(bookId), eq(null), anyString())).thenReturn(100L);

        mockMvc.perform(post("/books/{bookId}/views", bookId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Book view recorded successfully."))
                .andExpect(jsonPath("$.data").value(100));

        verify(bookService).recordBookView(eq(bookId), eq(null), anyString());
    }

    @Test
    void markBookAsFavoured_whenNotFollowed_followsBook() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = buildVerifiedUser(userId);
        BookDTO bookDTO = buildBookDTO(bookId, "Book Title", null);

        when(userService.findUserByJwt(jwt)).thenReturn(user);
        when(bookService.getBookById(bookId)).thenReturn(bookDTO);
        when(bookService.markAsFavouriteBook(bookId, user)).thenReturn(true);

        mockMvc.perform(put("/api/books/follow/{bookId}", bookId)
                .header("Authorization", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book added to favourites."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void markBookAsFavoured_whenFollowed_unfollowsBook() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = buildVerifiedUser(userId);
        BookDTO bookDTO = buildBookDTO(bookId, "Book Title", null);

        when(userService.findUserByJwt(jwt)).thenReturn(user);
        when(bookService.getBookById(bookId)).thenReturn(bookDTO);
        when(bookService.markAsFavouriteBook(bookId, user)).thenReturn(false);

        mockMvc.perform(put("/api/books/follow/{bookId}", bookId)
                .header("Authorization", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book removed from favourites."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void checkBookLikedByUser_returnsLikeStatus() throws Exception {
        String jwt = "Bearer test-token";
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = buildVerifiedUser(userId);

        when(userService.findUserByJwt(jwt)).thenReturn(user);
        when(bookService.isBookLikedByUser(userId, bookId)).thenReturn(true);

        mockMvc.perform(get("/api/books/{bookId}/isLiked", bookId)
                .header("Authorization", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Like status retrieved successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getBookCount_returnsCount() throws Exception {
        when(bookService.getBookCount()).thenReturn(42L);

        mockMvc.perform(get("/books/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book count retrieved successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(42));
    }

    @Test
    void getBookCommentCount_returnsCount() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(bookService.getCommentCountForBook(bookId)).thenReturn(15L);

        mockMvc.perform(get("/books/{bookId}/comments-count", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment count retrieved successfully."))
                .andExpect(jsonPath("$.data").value(15));
    }

    @Test
    void setEditorChoice_updatesBookStatus() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        BookDTO bookDTO = buildBookDTO(bookId, "Editor's Choice Book", authorId);
        bookDTO.setSuggested(true);

        when(bookService.setEditorChoice(eq(bookId), any(BookDTO.class))).thenReturn(bookDTO);
        when(userService.findUserById(authorId)).thenReturn(buildVerifiedUser(authorId));

        mockMvc.perform(put("/admin/books/{bookId}/editor-choice", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book set as editor's choice successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.suggested").value(true));
    }

    @Test
    void getUserFavouredBooks_returnsFavouredBooks() throws Exception {
        String jwt = "Bearer test-token";
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        User user = buildVerifiedUser(userId);
        BookDTO bookDTO = buildBookDTO(bookId, "Favoured Book", null);
        Page<BookDTO> page = new PageImpl<>(Collections.singletonList(bookDTO));

        when(userService.findUserByJwt(jwt)).thenReturn(user);
        when(userService.getUserIdsBlocking(userId)).thenReturn(Set.of());
        when(userService.getBlockedUserIds(userId)).thenReturn(Set.of());
        when(bookService.getFollowedBooksByUserId(eq(userId), any(Pageable.class), anySet())).thenReturn(page);

        mockMvc.perform(get("/api/books/favoured")
                .header("Authorization", jwt)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Favoured Book"));
    }

    @Test
    void getBooksByCategory_returnsCategoryBooks() throws Exception {
        Integer categoryId = 5;
        UUID bookId = UUID.randomUUID();
        BookDTO bookDTO = buildBookDTO(bookId, "Category Book", null);
        Page<BookDTO> page = new PageImpl<>(Collections.singletonList(bookDTO));

        when(bookService.getBooksByCategoryId(eq(categoryId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/categories/{categoryId}/books", categoryId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Category Book"));
    }

    @Test
    void getRelatedBooks_returnsRelatedBooks() throws Exception {
        UUID bookId = UUID.randomUUID();
        List<Integer> tagIds = Arrays.asList(1, 2, 3);
        List<BookDTO> relatedBooks = Collections.singletonList(
                buildBookDTO(UUID.randomUUID(), "Related Book", null));

        when(bookService.getBookById(bookId)).thenReturn(buildBookDTO(bookId, "Base Book", null));
        when(bookService.getRelatedBooks(bookId, tagIds)).thenReturn(relatedBooks);

        mockMvc.perform(get("/books/{bookId}/related", bookId)
                .param("tagIds", "1", "2", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Related Book"));
    }

    @Test
    void getLatestUpdateBooks_returnsRecentBooks() throws Exception {
        List<BookDTO> recentBooks = Arrays.asList(
                buildBookDTO(UUID.randomUUID(), "Recent Book 1", null),
                buildBookDTO(UUID.randomUUID(), "Recent Book 2", null));

        when(bookService.getTopRecentChapterBooks(5)).thenReturn(recentBooks);

        mockMvc.perform(get("/books/latest-update")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Recent Book 1"))
                .andExpect(jsonPath("$[1].title").value("Recent Book 2"));
    }

    @Test
    void getBooksUploadedPerMonthCount_returnsMonthlyStats() throws Exception {
        List<Long> monthlyCounts = Arrays.asList(10L, 15L, 20L, 25L, 30L);

        when(bookService.getBookUploadedPerMonthNumber()).thenReturn(monthlyCounts);

        mockMvc.perform(get("/books/books-upload-per-month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Monthly upload counts retrieved successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value(10))
                .andExpect(jsonPath("$.data[4]").value(30));
    }

    private BookDTO buildBookDTO(UUID bookId, String title, UUID authorId) {
        BookDTO bookDTO = new BookDTO();
        bookDTO.setId(bookId);
        bookDTO.setTitle(title);
        bookDTO.setDescription("Test description");
        bookDTO.setUploadDate(LocalDateTime.of(2023, 1, 1, 12, 0));
        bookDTO.setViewCount(100);
        bookDTO.setStatus("ONGOING");
        bookDTO.setLanguage("English");

        if (authorId != null) {
            UserSummaryDTO author = new UserSummaryDTO();
            author.setId(authorId);
            author.setUsername("testauthor");
            bookDTO.setAuthor(author);
        }

        return bookDTO;
    }

    private User buildUser(UUID userId, String roleName) {
        User user = new User();
        user.setId(userId);
        user.setBanned(false);
        user.setIsSuspended(Boolean.FALSE);
        user.setIsVerified(Boolean.FALSE);
        if (roleName != null) {
            Role role = new Role();
            role.setName(roleName);
            user.setRole(role);
        }
        return user;
    }

    private User buildVerifiedUser(UUID userId) {
        User user = buildUser(userId, "USER");
        user.setIsVerified(Boolean.TRUE);
        return user;
    }
}
