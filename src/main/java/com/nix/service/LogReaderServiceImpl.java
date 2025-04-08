package com.nix.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class LogReaderServiceImpl implements LogReaderService{
	private static final String LOG_FILE_PATH = "logs/app.log";
    private static final int MAX_LINES = 100; // Number of recent lines to return

    public List<String> getRecentLogs() {
        try {
            List<String> allLines = Files.readAllLines(Paths.get(LOG_FILE_PATH));
            int fromIndex = Math.max(0, allLines.size() - MAX_LINES);
            return allLines.subList(fromIndex, allLines.size());
        } catch (IOException e) {
            return Collections.singletonList("Error reading logs: " + e.getMessage());
        }
    }
}
