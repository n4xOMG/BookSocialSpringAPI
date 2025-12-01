package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookPerformanceDTO {
    private UUID bookId;
    private String title;
    private ImageAttachmentDTO bookCover;

    private long currentViews;
    private long currentFavourites;
    private long currentComments;

    private long dailyViewsGrowth;
    private long weeklyViewsGrowth;
    private long monthlyViewsGrowth;
    private long dailyFavouritesGrowth;
    private long weeklyFavouritesGrowth;
    private long monthlyFavouritesGrowth;
    private long dailyCommentsGrowth;
    private long weeklyCommentsGrowth;
    private long monthlyCommentsGrowth;

    private String status;
    private LocalDateTime lastUpdated;
    private int totalChapters;
}