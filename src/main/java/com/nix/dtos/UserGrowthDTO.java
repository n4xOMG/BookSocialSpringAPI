package com.nix.dtos;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGrowthDTO {
	private LocalDate date;
	private Long newUsers;
	private Long totalUsers;
}