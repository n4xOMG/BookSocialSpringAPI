package com.nix.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.nix.models.Chapter;

public interface ChapterService {
	public Chapter findChapterById(UUID chapterId);

	public void processChaptersByEpubFile(UUID bookId, InputStream inputStream, Integer startByChapterNum)
			throws IOException;

	public void processChaptersByDocFile(UUID bookId, InputStream inputStream, Integer startByChapterNum)
			throws IOException;

	public List<Chapter> findChaptersByBookId(UUID bookId);

	public List<Chapter> findChaptersByBookId(UUID bookId, String sortBy, String sortDir);

	public List<Chapter> findNotDraftedChaptersByBookId(UUID bookId);

	public List<Chapter> findNotDraftedChaptersByBookId(UUID bookId, String sortBy, String sortDir);

	public List<Chapter> getAllChapters();

	public Chapter getChapterByRoomId(String roomId);

	public Chapter createDraftChapter(UUID bookId, Chapter chapter);

	public Chapter publishChapter(UUID bookId, Chapter chapter);

	public Chapter editChapter(UUID chapterId, Chapter chapter) throws Exception;

	public String deleteChapter(UUID chapterId) throws Exception;

	public void unlockChapter(UUID userId, UUID chapterId) throws Exception;

	public boolean isChapterUnlockedByUser(UUID userId, UUID chapterId);

	public Boolean likeChapter(UUID userId, UUID chapterId) throws Exception;

	public Chapter unlikeChapter(UUID userId, UUID chapterId) throws Exception;

	public boolean isChapterLikedByUser(UUID userId, UUID chapterId);

}
