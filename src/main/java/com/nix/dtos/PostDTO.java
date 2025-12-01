package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private UUID id;
    private UserSummaryDTO user;
    private String content;
    private UUID sharedPostId;
    private List<ImageAttachmentDTO> images = new ArrayList<>();
    private Integer likes;
    private Integer commentCount;
    private LocalDateTime timestamp;
    private UserSummaryDTO sharedPostUser;
    private List<ImageAttachmentDTO> sharedPostImages = new ArrayList<>();
    private String sharedPostContent;
    private boolean likedByCurrentUser;
    private LocalDateTime sharedPostTimestamp;

    private BookDTO sharedBook;
    private ChapterSummaryDTO sharedChapter;
    private PostType postType = PostType.STANDARD; // STANDARD, BOOK_SHARE, CHAPTER_SHARE

    // Enum for post types
    public enum PostType {
        STANDARD, BOOK_SHARE, CHAPTER_SHARE
    }
}
