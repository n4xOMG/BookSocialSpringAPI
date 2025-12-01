package com.nix.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
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

import com.nix.dtos.ReportDTO;
import com.nix.exception.ResourceNotFoundException;
import com.nix.exception.UnauthorizedException;
import com.nix.response.ApiResponseWithData;
import com.nix.service.ReportService;

@RestController
@RequestMapping("/api/reports")
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    // Create a new report
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseWithData<ReportDTO>> createReport(@RequestHeader("Authorization") String jwt,
            @RequestBody ReportDTO reportDTO) {
        try {
            ReportDTO createdReport = reportService.createReport(jwt, reportDTO);
            return buildSuccessResponse(HttpStatus.CREATED, "Report created successfully.", createdReport);
        } catch (UnauthorizedException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to create report", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create report.");
        }
    }

    // Get all reports (Admin Only)
    @GetMapping
    public ResponseEntity<ApiResponseWithData<List<ReportDTO>>> getAllReports() {
        try {
            List<ReportDTO> reports = reportService.getAllReports();
            return buildSuccessResponse("Reports retrieved successfully.", reports);
        } catch (Exception e) {
            logger.error("Failed to retrieve reports", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve reports.");
        }
    }

    // Get report count
    @GetMapping("/count")
    public ResponseEntity<ApiResponseWithData<Long>> getReportCount() {
        try {
            Long count = reportService.getReportsCount();
            return buildSuccessResponse("Report count retrieved successfully.", count);
        } catch (Exception e) {
            logger.error("Failed to retrieve report count", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve report count.");
        }
    }

    // Get report by ID (Admin Only)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWithData<ReportDTO>> getReportById(@PathVariable UUID id) {
        try {
            ReportDTO report = reportService.getReportById(id);
            return buildSuccessResponse("Report retrieved successfully.", report);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to retrieve report {}", id, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve report.");
        }
    }

    // Resolve a report (Admin Only)
    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponseWithData<Void>> resolveReport(@PathVariable UUID id) {
        try {
            reportService.resolveReport(id);
            return this.<Void>buildSuccessResponse("Report resolved successfully.", null);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to resolve report {}", id, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resolve report.");
        }
    }

    // Delete a report (Admin Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWithData<Void>> deleteReport(@PathVariable UUID id) {
        try {
            reportService.deleteReport(id);
            return this.<Void>buildSuccessResponse("Report deleted successfully.", null);
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to delete report {}", id, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete report.");
        }
    }

    // Delete reported object (Admin Only)
    @DeleteMapping("/{id}/delete-object")
    public ResponseEntity<ApiResponseWithData<Void>> deleteReportedObject(@PathVariable UUID id,
            @RequestHeader("Authorization") String jwt) {
        try {
            reportService.deleteReportedObject(id, jwt);
            return this.<Void>buildSuccessResponse("Reported object deleted successfully.", null);
        } catch (UnauthorizedException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (NotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to delete reported object for report {}", id, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete reported object.");
        }
    }

    private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
        return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
    }

    private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
    }

    private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false, null));
    }
}
