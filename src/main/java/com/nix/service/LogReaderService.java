package com.nix.service;

import com.nix.dtos.LogPageResponse;

public interface LogReaderService {
	LogPageResponse getRecentLogs(int page, int size);

	LogPageResponse getLogsByUsername(String username, int page, int size);
}
