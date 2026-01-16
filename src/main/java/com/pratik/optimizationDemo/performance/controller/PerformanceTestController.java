package com.pratik.optimizationDemo.performance.controller;

import com.pratik.optimizationDemo.performance.dao.ReportDao;
import com.pratik.optimizationDemo.performance.model.MetricAggregate;
import com.pratik.optimizationDemo.performance.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/performance")
public class PerformanceTestController {

    private final ReportDao reportDao;
    private final ReportService reportService;

    public PerformanceTestController(ReportDao reportDao, ReportService reportService) {
        this.reportDao = reportDao;
        this.reportService = reportService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/batch/optimized")
    public ResponseEntity<List<MetricAggregate>> optimizedBatch(
            @RequestParam Long tenantId,
            @RequestParam String groupId,
            @RequestParam(defaultValue = "") String lastSeenId,
            @RequestParam(defaultValue = "1000") int limit) {

        return ResponseEntity.ok(reportDao.fetchAggregatesOptimized(tenantId, groupId, lastSeenId, limit));
    }

    @GetMapping("/batch/slow")
    public ResponseEntity<List<MetricAggregate>> slowBatch(
            @RequestParam Long tenantId,
            @RequestParam String groupId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "1000") int limit) {

        return ResponseEntity.ok(reportDao.fetchAggregatesSlow(tenantId, groupId, offset, limit));
    }

    @GetMapping("/report/optimized")
    public ResponseEntity<List<MetricAggregate>> optimizedReport(
            @RequestParam Long tenantId,
            @RequestParam String groupId,
            @RequestParam(defaultValue = "1000") int batchSize) {

        return ResponseEntity.ok(reportService.generateReport(tenantId, groupId, batchSize));
    }
}
