package com.nix.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalyticsDTO {
	private Long totalUsers;
	private Long activeUsers; // Users active in last 30 days
	private Long newUsersThisMonth;
	private Long bannedUsers;
	private Long suspendedUsers;
	private List<UserGrowthDTO> userGrowthHistory; // Monthly growth
}