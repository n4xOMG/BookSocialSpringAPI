package com.nix.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopSpenderAnalyticsDTO {
    private UUID userId;
    private String username;
    private String fullname;
    private String avatarUrl;
    private BigDecimal totalSpent;
    private Long transactionCount;
    private Long creditsPurchased;
}
