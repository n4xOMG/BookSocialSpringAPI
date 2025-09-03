package com.nix.controller;

import java.util.List;
import java.util.UUID;

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
import com.nix.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;


    // Create a new report
    @PostMapping
    public ResponseEntity<?> createReport(@RequestHeader("Authorization") String jwt,
                                         @RequestBody ReportDTO reportDTO) {
        try {
            ReportDTO createdReport = reportService.createReport(jwt, reportDTO);
            return new ResponseEntity<>(createdReport, HttpStatus.CREATED);
        } catch (UnauthorizedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get all reports (Admin Only)
    @GetMapping
    public ResponseEntity<?> getAllReports() {
        try {
            List<ReportDTO> reports = reportService.getAllReports();
            return new ResponseEntity<>(reports, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get report count
    @GetMapping("/count")
    public ResponseEntity<?> getReportCount() {
        try {
            Long count = reportService.getReportsCount();
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get report by ID (Admin Only)
    @GetMapping("/{id}")
    public ResponseEntity<?> getReportById(@PathVariable UUID id) {
        try {
            ReportDTO report = reportService.getReportById(id);
            return new ResponseEntity<>(report, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Resolve a report (Admin Only)
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable UUID id) {
        try {
            reportService.resolveReport(id);
            return new ResponseEntity<>("Report resolved successfully.", HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Delete a report (Admin Only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable UUID id) {
        try {
            reportService.deleteReport(id);
            return new ResponseEntity<>("Report deleted successfully.", HttpStatus.NO_CONTENT);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Delete reported object (Admin Only)
    @DeleteMapping("/{id}/delete-object")
    public ResponseEntity<?> deleteReportedObject(@PathVariable UUID id, @RequestHeader("Authorization") String jwt) {
        try {
            reportService.deleteReportedObject(id, jwt);
            return new ResponseEntity<>("Reported object deleted successfully.", HttpStatus.NO_CONTENT);
        } catch (UnauthorizedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
