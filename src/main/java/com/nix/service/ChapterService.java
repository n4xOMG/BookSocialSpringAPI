package com.nix.service;

import java.util.List;

import com.nix.models.Chapter;

public interface ChapterService {
	public Chapter findChapterById(Long chapterId);

	public List<Chapter> findChaptersByBookId(Long bookId);

	public List<Chapter> findNotDraftedChaptersByBookId(Long bookId);

	public List<Chapter> getAllChapters();

	public Chapter getChapterByRoomId(String roomId);

	public Chapter createDraftChapter(Long bookId, Chapter chapter);

	public Chapter publishChapter(Long bookId, Chapter chapter);

	public Chapter editChapter(Long chapterId, Chapter chapter) throws Exception;

	public String deleteChapter(Long chapterId) throws Exception;

	public void unlockChapter(Long userId, Long chapterId) throws Exception;

	public boolean isChapterUnlockedByUser(Long userId, Long chapterId);

	public Boolean likeChapter(Long userId, Long chapterId) throws Exception;

	public Chapter unlikeChapter(Long userId, Long chapterId) throws Exception;

	public boolean isChapterLikedByUser(Long userId, Long chapterId);

}
