package com.example.grocerystore.controller;

import com.example.grocerystore.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/admin/reports/monthly")
    public ResponseEntity<byte[]> monthlyReport(@RequestParam(required = false) String month) {
        try {
            YearMonth ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
            int year = ym.getYear();
            int m = ym.getMonthValue();
            byte[] data = reportService.generateMonthlySalesReport(year, m);
            String filename = String.format("monthly-sales-%04d-%02d.xlsx", year, m);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(null);
        }
    }
}