package com.nix.dtos.mappers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.BookDTO;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Rating;

public class BookMapper implements Mapper<Book, BookDTO> {
	CommentMapper commentMapper = new CommentMapper();

	RatingMapper ratingMapper = new RatingMapper();

	ChapterMapper chapterMapper = new ChapterMapper();

	CategoryMapper categoryMapper = new CategoryMapper();

	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	@Override
	public BookDTO mapToDTO(Book book) {
		BookDTO bookDTO = new BookDTO();
		if (book.getId() != null) {
			bookDTO.setId(book.getId());
		}
		bookDTO.setTitle(book.getTitle());
		bookDTO.setAuthor(userSummaryMapper.mapToDTO(book.getAuthor()));
		bookDTO.setArtistName(book.getArtistName());
		bookDTO.setBookCover(book.getBookCover());
		bookDTO.setDescription(book.getDescription());
		bookDTO.setUploadDate(book.getUploadDate());
		bookDTO.setViewCount(book.getViewCount());
		bookDTO.setSuggested(book.isSuggested());
		
		if (book.getRatings() != null && !book.getRatings().isEmpty()) {
			double avgRating = book.getRatings().stream().mapToDouble(Rating::getRating).average().orElse(0.0);
			bookDTO.setAvgRating(avgRating);
			bookDTO.setRatingCount(book.getRatings().size());
		} else {
			bookDTO.setAvgRating(0.0);
			bookDTO.setRatingCount(0);
		}
		if (book.getFavoured() != null && !book.getFavoured().isEmpty()) {
			bookDTO.setFavCount(book.getFavoured().size());
		}

		bookDTO.setCategories(categoryMapper.mapToDTOs(book.getCategories()));
		bookDTO.setChapterCount(book.getChapters().size());
		bookDTO.setLanguage(book.getLanguage());
		book.getChapters().stream().max(Comparator.comparing(Chapter::getUploadDate))
				.ifPresent(latestChapter -> bookDTO.setLatestChapterNumber(latestChapter.getChapterNum()));
		return bookDTO;
	}

	@Override
	public List<BookDTO> mapToDTOs(List<Book> books) {
		return books.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
