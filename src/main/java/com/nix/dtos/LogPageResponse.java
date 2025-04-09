package com.nix.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogPageResponse {
	private List<String> logs;
    private int page;
    private int size;
    private boolean hasNext;
    private long totalElements;
}
