package com.nix.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.nix.dtos.ReportDTO;
import com.nix.dtos.mappers.ReportMapper;
import com.nix.enums.NotificationEntityType;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Comment;
import com.nix.models.Report;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.ReportRepository;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
import com.nix.service.CommentService;
import com.nix.service.NotificationService;
import com.nix.service.ReportService;
import com.nix.service.UserService;

@Service
public class ReportServiceImpl implements ReportService {

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookService bookService;

	@Autowired
	NotificationService notificationService;

	@Autowired
	private ChapterService chapterService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private ReportMapper reportMapper;

	@Override
	public ReportDTO createReport(String jwt, ReportDTO reportDTO) throws Exception {
		// Get User from JWT
		User reporter = userService.findUserByJwt(jwt);
		if (reporter == null) {
			throw new Exception("User has not logged in!");
		}

		Report report = new Report();
		report.setReason(reportDTO.getReason());
		report.setReporter(reporter);
		report.setReportedDate(LocalDateTime.now());
		report.setResolved(false);

		// Associate the report with the relevant entity
		if (reportDTO.getComment() != null) {
			Comment comment = commentService.findCommentById(reportDTO.getComment().getId());
			if (comment == null) {
				throw new NotFoundException();
			}
			report.setComment(comment);
		}

		if (reportDTO.getBook() != null) {
			Optional<Book> book = bookRepository.findById(reportDTO.getBook().getId());
			if (book == null) {
				throw new NotFoundException();
			}
			report.setBook(book.get());
		}

		if (reportDTO.getChapter() != null) {
			Chapter chapter = chapterService.findChapterById(reportDTO.getChapter().getId());
			if (chapter == null) {
				throw new NotFoundException();
			}
			report.setChapter(chapter);
		}

		Report createdReport = reportRepository.save(report);
		return reportMapper.mapToDTO(createdReport);
	}

	@Override
	public List<ReportDTO> getAllReports() {
		List<Report> reports = reportRepository.findAll();
		return reportMapper.mapToDTOs(reports);
	}

	@Override
	public Long getReportsCount() {
		return reportRepository.count();
	}

	@Override
	public ReportDTO getReportById(UUID id) throws Exception {
		Optional<Report> optionalReport = reportRepository.findById(id);
		if (optionalReport.isPresent()) {
			return reportMapper.mapToDTO(optionalReport.get());
		}
		throw new NotFoundException();
	}

	@Override
	public void resolveReport(UUID id) throws Exception {
		Report report = getReportEntityById(id);
		report.setResolved(true);
		reportRepository.save(report);
		String message = "Your report has been resolved";
		notificationService.createNotification(report.getReporter(), message, NotificationEntityType.REPORT,
				report.getId());
	}

	@Override
	public void deleteReport(UUID id) throws Exception {
		Report report = getReportEntityById(id);
		reportRepository.delete(report);
	}

	@Override
	public void deleteReportedObject(UUID id, String jwt) throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("User has not logged in!");
		}

		Report report = getReportEntityById(id);
		if (report.getBook() != null) {
			UUID bookId = report.getBook().getId();
			report.setBook(null);
			reportRepository.save(report);
			bookService.deleteBook(bookId);
		}
		if (report.getChapter() != null) {
			UUID chapterId = report.getChapter().getId();
			report.setChapter(null);
			reportRepository.save(report);
			chapterService.deleteChapter(chapterId);
		}
		if (report.getComment() != null) {
			UUID commentId = report.getComment().getId();
			report.setComment(null);
			reportRepository.save(report);
			commentService.deleteComment(commentId, user.getId());
		}
		resolveReport(id);
		deleteReport(id);
	}

	@Override
	public Report saveReport(Report report) {
		return reportRepository.save(report);
	}

	private Report getReportEntityById(UUID id) throws NotFoundException {
		Optional<Report> optionalReport = reportRepository.findById(id);
		if (optionalReport.isPresent()) {
			return optionalReport.get();
		}
		throw new NotFoundException();
	}
}
