package com.nix.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.nix.dtos.LogPageResponse;
import com.nix.service.LogReaderService;

@Service
public class LogReaderServiceImpl implements LogReaderService {
	private static final String LOG_FILE_PATH = "logs/app.log";
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	@Override
	public LogPageResponse getRecentLogs(int page, int size) {
		size = validatePageSize(size);

		try {
			List<String> allLines = Files.readAllLines(Paths.get(LOG_FILE_PATH));
			int totalElements = allLines.size();

			// Calculate start and end index for pagination
			int fromIndex = Math.max(0, totalElements - (page + 1) * size);
			int toIndex = Math.max(0, totalElements - page * size);

			// For logs, usually we want to show newest first
			List<String> pagedLogs;
			if (fromIndex < toIndex) {
				pagedLogs = allLines.subList(fromIndex, toIndex);
				Collections.reverse(pagedLogs); // Reverse to get newest first
			} else {
				pagedLogs = Collections.emptyList();
			}

			boolean hasNext = fromIndex > 0;

			return new LogPageResponse(pagedLogs, page, size, hasNext, totalElements);
		} catch (IOException e) {
			return new LogPageResponse(Collections.singletonList("Error reading logs: " + e.getMessage()), page, size,
					false, 0);
		}
	}

	@Override
	public LogPageResponse getLogsByUsername(String email, int page, int size) {
		size = validatePageSize(size);

		try {
			List<String> allLines = Files.readAllLines(Paths.get(LOG_FILE_PATH));

			// Filter logs for the specified user
			List<String> userLogs = allLines.stream().filter(line -> line.contains("User=" + email))
					.collect(Collectors.toList());

			int totalElements = userLogs.size();

			// Calculate pagination
			int fromIndex = page * size;
			int toIndex = Math.min(fromIndex + size, totalElements);

			List<String> pagedLogs = fromIndex < totalElements ? userLogs.subList(fromIndex, toIndex)
					: Collections.emptyList();

			boolean hasNext = toIndex < totalElements;

			return new LogPageResponse(
					pagedLogs.isEmpty() ? Collections.singletonList("No logs found for user: " + email) : pagedLogs,
					page, size, hasNext, totalElements);
		} catch (IOException e) {
			return new LogPageResponse(Collections.singletonList("Error reading logs: " + e.getMessage()), page, size,
					false, 0);
		}
	}

	private int validatePageSize(int size) {
		if (size <= 0) {
			return DEFAULT_PAGE_SIZE;
		} else if (size > MAX_PAGE_SIZE) {
			return MAX_PAGE_SIZE;
		}
		return size;
	}
}
