package com.nix.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.BookDTO;
import com.nix.dtos.ReportDTO;
import com.nix.dtos.mappers.ReportMapper;
import com.nix.models.Chapter;
import com.nix.models.Comment;
import com.nix.models.Report;
import com.nix.models.User;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
import com.nix.service.CommentService;
import com.nix.service.ReportService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

	@Autowired
	private ReportService reportService;

	@Autowired
	private UserService userService;

	@Autowired
	private BookService bookService;

	@Autowired
	private ChapterService chapterService;

	@Autowired
	private CommentService commentService;

	ReportMapper reportMapper = new ReportMapper();

	// Create a new report
	@PostMapping
	public ResponseEntity<?> createReport(@RequestHeader("Authorization") String jwt,
			@RequestBody ReportDTO reportDTO) {
		try {
			// Get User from JWT
			User reporter = userService.findUserByJwt(jwt);
			if (reporter == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
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
					return new ResponseEntity<>("Comment not found!", HttpStatus.BAD_REQUEST);
				}
				report.setComment(comment);
			}

			if (reportDTO.getBook() != null) {
				BookDTO book = bookService.getBookById(reportDTO.getBook().getId());
				if (book == null) {
					return new ResponseEntity<>("Book not found!", HttpStatus.BAD_REQUEST);
				}
			}

			if (reportDTO.getChapter() != null) {
				Chapter chapter = chapterService.findChapterById(reportDTO.getChapter().getId());
				if (chapter == null) {
					return new ResponseEntity<>("Chapter not found!", HttpStatus.BAD_REQUEST);
				}
				report.setChapter(chapter);
			}

			Report createdReport = reportService.createReport(report);
			return new ResponseEntity<>(reportMapper.mapToDTO(createdReport), HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Get all reports (Admin Only)
	@GetMapping
	public ResponseEntity<?> getAllReports() {
		try {
			List<Report> reports = reportService.getAllReports();
			return new ResponseEntity<>(reportMapper.mapToDTOs(reports), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Get report by ID (Admin Only)
	@GetMapping("/{id}")
	public ResponseEntity<?> getReportById(@PathVariable Long id) {
		try {
			Report report = reportService.getReportById(id);
			return new ResponseEntity<>(reportMapper.mapToDTO(report), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	// Resolve a report (Admin Only)
	@PutMapping("/{id}/resolve")
	public ResponseEntity<?> resolveReport(@PathVariable Long id) {
		try {
			reportService.resolveReport(id);
			return new ResponseEntity<>("Report resolved successfully.", HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	// Delete a report (Admin Only)
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteReport(@PathVariable Long id) {
		try {
			reportService.deleteReport(id);
			return new ResponseEntity<>("Report deleted successfully.", HttpStatus.NO_CONTENT);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@DeleteMapping("/{id}/delete-object")
	public ResponseEntity<?> deleteReportedObject(@PathVariable Long id, @RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			Report report = reportService.getReportById(id);
			if (report != null) {
				if (report.getBook() != null) {
					Integer bookId = report.getBook().getId();
					report.setBook(null);
					reportService.saveReport(report);
					bookService.deleteBook(bookId);
				}
				if (report.getChapter() != null) {
					Integer chapterId = report.getChapter().getId();
					report.setChapter(null);
					reportService.saveReport(report);
					chapterService.deleteChapter(chapterId);
				}
				if (report.getComment() != null) {
					Integer commentId = report.getComment().getId();
					report.setComment(null);
					reportService.saveReport(report);
					commentService.deleteComment(commentId, user.getId());
				}
				resolveReport(id);
				reportService.deleteReport(id);
			}
			return new ResponseEntity<>("Reported object deleted successfully.", HttpStatus.NO_CONTENT);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}
}
