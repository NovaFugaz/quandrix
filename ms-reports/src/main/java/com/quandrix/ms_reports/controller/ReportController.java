package com.quandrix.ms_reports.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quandrix.ms_reports.dto.SalesReportResponse;
import com.quandrix.ms_reports.dto.TopCardResponse;
import com.quandrix.ms_reports.dto.TopSellerResponse;
import com.quandrix.ms_reports.service.ReportService;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService){
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        log.info("GET /reports/sales from={} to={}", from, to);
        return ResponseEntity.ok(reportService.getSalesReport(from, to));
    }

    @GetMapping("/top-sellers")
    public ResponseEntity<List<TopSellerResponse>> getTopSellers(
        @RequestParam(defaultValue = "10") int limit){
            log.info("GET /reports/top-sellers limit={}", limit);
            return ResponseEntity.ok(reportService.getTopSellers(limit));
        }
    

    @GetMapping("/top-cards")
    public ResponseEntity<List<TopCardResponse>> getTopCards(
        @RequestParam(defaultValue = "10") int limit){
            log.info("GET /reports/top-cards limit={}", limit);
            return ResponseEntity.ok(reportService.getTopCards(limit));
        }

    @GetMapping("/sumary")
    public ResponseEntity<SalesReportResponse> getSumary(){
            log.info("GET /reports/summary");
            return ResponseEntity.ok(reportService.getGeneralSummary());
        }
}
