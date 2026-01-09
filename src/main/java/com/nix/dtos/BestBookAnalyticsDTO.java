package com.nix.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BestBookAnalyticsDTO {
    private UUID bookId;
    private String title;
    private String authorName;
    private String coverImage;
    private Long viewCount;
    private Long favouriteCount;
    private Double averageRating;
}
