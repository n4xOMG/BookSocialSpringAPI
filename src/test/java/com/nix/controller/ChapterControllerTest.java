package com.nix.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Role;
import com.nix.models.User;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
import com.nix.service.PaymentService;
import com.nix.service.ReadingProgressService;
import com.nix.service.UserService;

@WebMvcTest(ChapterController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChapterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChapterService chapterService;

    @MockBean
    private BookService bookService;

    @MockBean
    private UserService userService;

    @MockBean
    private ReadingProgressService progressService;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getChapterById_whenLockedAndNotUnlocked_returnsSummary() throws Exception {
        UUID chapterId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Chapter chapter = buildChapter(chapterId, bookId, null, true, 25, "Locked content");
        when(chapterService.findChapterById(chapterId)).thenReturn(chapter);

        mockMvc.perform(get("/chapters/{chapterId}", chapterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chapter summary retrieved successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(chapterId.toString()))
                .andExpect(jsonPath("$.data.bookId").value(chapter.getBook().getId().toString()))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.title").value("Chapter Title"));
    }

    @Test
    void getChapterById_whenUserUnlocked_returnsFullChapter() throws Exception {
        UUID chapterId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Chapter chapter = buildChapter(chapterId, bookId, authorId, false, 0, "Full chapter content");
        String jwt = "Bearer test-token";
        User currentUser = buildUser(UUID.randomUUID(), "USER");

        when(chapterService.findChapterById(chapterId)).thenReturn(chapter);
        when(userService.findUserByJwt(jwt)).thenReturn(currentUser);
        when(userService.isBlockedBy(currentUser.getId(), authorId)).thenReturn(false);
        when(userService.hasBlocked(currentUser.getId(), authorId)).thenReturn(false);
        when(chapterService.isChapterUnlockedByUser(currentUser.getId(), chapterId)).thenReturn(true);
        when(chapterService.isChapterLikedByUser(currentUser.getId(), chapterId)).thenReturn(true);

        mockMvc.perform(get("/chapters/{chapterId}", chapterId).header("Authorization", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chapter retrieved successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(chapterId.toString()))
                .andExpect(jsonPath("$.data.bookId").value(bookId.toString()))
                .andExpect(jsonPath("$.data.content").value("Full chapter content"))
                .andExpect(jsonPath("$.data.unlockedByUser").value(true))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.uploadDate").value(chapter.getUploadDate().toString()));
    }

    @Test
    void likeChapter_whenServiceMarksAsLiked_returnsUpdatedDto() throws Exception {
        UUID chapterId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Chapter chapter = buildChapter(chapterId, bookId, null, false, 0, "Content to like");
        String jwt = "Bearer like-token";
        User currentUser = buildUser(UUID.randomUUID(), "USER");

        when(userService.findUserByJwt(jwt)).thenReturn(currentUser);
        when(chapterService.findChapterById(chapterId)).thenReturn(chapter);
        when(chapterService.likeChapter(currentUser.getId(), chapterId)).thenReturn(true);

        mockMvc.perform(put("/api/chapters/{chapterId}/like", chapterId).header("Authorization", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chapter liked successfully."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(chapterId.toString()))
                .andExpect(jsonPath("$.data.bookId").value(bookId.toString()))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
    }

    private Chapter buildChapter(UUID chapterId, UUID bookId, UUID authorId, boolean locked, int price,
            String content) {
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setRoomId("room-" + chapterId.toString());
        chapter.setChapterNum("1");
        chapter.setTitle("Chapter Title");
        chapter.setPrice(price);
        chapter.setLocked(locked);
        chapter.setDraft(false);
        chapter.setContent(content);
        chapter.setUploadDate(LocalDateTime.of(2023, 1, 1, 12, 0));

        Book book = new Book();
        book.setId(bookId);
        if (authorId != null) {
            User author = new User();
            author.setId(authorId);
            book.setAuthor(author);
        }
        chapter.setBook(book);
        return chapter;
    }

    private User buildUser(UUID userId, String roleName) {
        User user = new User();
        user.setId(userId);
        user.setBanned(false);
        user.setIsSuspended(Boolean.FALSE);
        user.setIsVerified(Boolean.TRUE);
        if (roleName != null) {
            Role role = new Role();
            role.setName(roleName);
            user.setRole(role);
        }
        return user;
    }
}
