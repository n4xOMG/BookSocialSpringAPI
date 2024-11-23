package com.nix.service;

import java.util.List;

import com.nix.models.Chapter;

public interface ChapterService {
	public Chapter findChapterById(Integer chapterId) throws Exception;

	public List<Chapter> findChaptersByBookId(Integer bookId);

	public List<Chapter> getAllChapters();

	public Chapter addChapterAndNotifyFollowers(Integer bookId, Chapter chapter);

	public Chapter editChapter(Integer chapterId, Chapter chapter) throws Exception;

	public String deleteChapter(Integer chapterId) throws Exception;

	public Chapter findChapterDTOById(Integer chapterId) throws Exception;

	public void incrementChapterViewCount(Integer chapterId);

	public void unlockChapter(Integer userId, Integer chapterId) throws Exception;

}
