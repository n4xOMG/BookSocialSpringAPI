package com.nix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

}
