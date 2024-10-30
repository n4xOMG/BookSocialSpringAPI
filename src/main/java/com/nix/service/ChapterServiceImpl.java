package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.repository.BookRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.ReadingProgressRepository;

@Service
public class ChapterServiceImpl implements ChapterService {

	@Autowired
	ChapterRepository chapterRepo;

	@Autowired
	BookRepository bookRepo;

	@Autowired
	ReadingProgressRepository progressRepo;

	@Override
	public Chapter findChapterById(Integer chapterId) throws Exception {
		Optional<Chapter> chapter = chapterRepo.findById(chapterId);

		if (chapter != null) {
			return chapter.get();
		}
		throw new Exception("No chapter found with id: " + chapterId);
	}

	@Override
	public Chapter findChapterDTOById(Integer chapterId) throws Exception {
		Optional<Chapter> chapter = chapterRepo.findById(chapterId);

		if (chapter != null) {
			return chapter.get();
		}
		throw new Exception("No chapter found with id: " + chapterId);
	}

	@Override
	public List<Chapter> findChaptersByBookId(Integer bookId) {
		return chapterRepo.findByBookId(bookId);
	}

	@Override
	public List<Chapter> getAllChapters() {
		return chapterRepo.findAll();
	}

	@Override
	@Transactional
	public Chapter addNewChapter(Integer bookId, Chapter chapter) throws Exception {
		Optional<Book> book = bookRepo.findById(bookId);
		if (!book.isPresent()) {
			throw new Exception("Book not found");
		}

		chapter.setBook(book.get());
		chapter.setUploadDate(LocalDateTime.now());

		Chapter newChapter = chapterRepo.save(chapter);

		return newChapter;
	}

	@Override
	public Chapter editChapter(Integer chapterId, Chapter chapter) throws Exception {
		Chapter editChapter = findChapterById(chapterId);
		if (editChapter == null) {
			throw new Exception("Chapter not found");
		}

		if (chapter.getChapterNum() != null) {
			editChapter.setChapterNum(chapter.getChapterNum());
		}

		if (chapter.getTitle() != null) {
			editChapter.setTitle(chapter.getTitle());
		}

		if (chapter.getContent() != null) {
			editChapter.setContent(chapter.getContent());
		}

		if (chapter.getTranslatorId() != null) {
			editChapter.setTranslatorId(chapter.getTranslatorId());
		}

		return chapterRepo.save(editChapter);
	}

	@Override
	@Transactional
	public String deleteChapter(Integer chapterId) throws Exception {
		Chapter deleteChapter = findChapterById(chapterId);
		if (deleteChapter == null) {
			throw new Exception("Chapter not found");
		}

		try {
			deleteChapter.setBook(null);
			deleteChapter.setTranslatorId(null);
			progressRepo.deleteByChapterId(chapterId);

			chapterRepo.delete(deleteChapter);

			return "Chapter deleted successfully!";
		} catch (Exception e) {
			return "Error deleting chapter: " + e.getMessage();
		}
	}

	@Override
	public void incrementChapterViewCount(Integer chapterId) {
		chapterRepo.incrementViewCount(chapterId);
		
	}


}
