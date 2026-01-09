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
public class ActiveUserAnalyticsDTO {
    private UUID userId;
    private String username;
    private String fullname;
    private String avatarUrl;
    private Long commentCount;
    private Long readCount;
    private Long activityScore; // Weighted: comments * 3 + reads * 1
}
