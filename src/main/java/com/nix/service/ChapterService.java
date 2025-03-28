package com.nix.service;

import java.util.List;

import com.nix.models.Chapter;

public interface ChapterService {
	public Chapter findChapterById(Integer chapterId);

	public List<Chapter> findChaptersByBookId(Integer bookId);

	public List<Chapter> findNotDraftedChaptersByBookId(Integer bookId);

	public List<Chapter> getAllChapters();

	public Chapter getChapterByRoomId(String roomId);

	public Chapter createDraftChapter(Integer bookId, Chapter chapter);

	public Chapter publishChapter(Integer bookId, Chapter chapter);

	public Chapter editChapter(Integer chapterId, Chapter chapter) throws Exception;

	public String deleteChapter(Integer chapterId) throws Exception;

	public void unlockChapter(Integer userId, Integer chapterId) throws Exception;

	public boolean isChapterUnlockedByUser(Integer userId, Integer chapterId);

	public Boolean likeChapter(Integer userId, Integer chapterId) throws Exception;

	public Chapter unlikeChapter(Integer userId, Integer chapterId) throws Exception;

	public boolean isChapterLikedByUser(Integer userId, Integer chapterId);

}
