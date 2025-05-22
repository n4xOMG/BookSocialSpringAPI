package com.nix.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SalesPerUserDTO {
	private Long userId;
	private String username;
	private Double totalSpent;
}
