package com.nix.service;

import java.util.List;

import com.nix.models.Report;

public interface ReportService {
	Report createReport(Report report) throws Exception;

	List<Report> getAllReports();

	Report getReportById(Long id) throws Exception;

	void resolveReport(Long id) throws Exception;

	void deleteReport(Long id) throws Exception;
}
