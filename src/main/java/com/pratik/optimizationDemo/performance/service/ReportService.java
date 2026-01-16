package com.pratik.optimizationDemo.performance.service;

import com.pratik.optimizationDemo.performance.dao.ReportDao;
import com.pratik.optimizationDemo.performance.model.MetricAggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service layer for compliance report generation with batch processing.
 *
 * This service demonstrates production-ready patterns for handling large datasets:
 *
 * 1. BATCH PROCESSING
 *    - Process data in controlled chunks (default 1000 records)
 *    - Prevents memory exhaustion on large result sets
 *    - Allows progress tracking and cancellation
 *
 * 2. KEYSET PAGINATION
 *    - Track lastSeenId between batches
 *    - O(1) seek to next batch vs O(n) offset scan
 *    - Consistent performance regardless of position
 *
 * 3. CALLBACK PATTERN
 *    - Process batches as they arrive
 *    - Enables streaming to clients or files
 *    - Reduces memory footprint
 *
 * NOTE: This is a representative example. Implementation details have been
 * generalized to preserve client confidentiality.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    // Batch size tuned for optimal performance vs memory trade-off
    // Too small: excessive round-trips to database
    // Too large: memory pressure and longer individual query times
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public List<MetricAggregate> generateReport(Long tenantId, String groupId) {
        return generateReport(tenantId, groupId, DEFAULT_BATCH_SIZE);
    }

    public List<MetricAggregate> generateReport(
            Long tenantId,
            String groupId,
            int batchSize) {

        log.info("Starting report generation for tenant={}, group={}, batchSize={}",
                tenantId, groupId, batchSize);

        List<MetricAggregate> allResults = new ArrayList<>();
        String lastSeenId = "";  // Start from beginning
        int batchNumber = 0;
        long totalRecords = 0;

        long startTime = System.currentTimeMillis();

        while (true) {
            batchNumber++;

            // Fetch next batch using keyset pagination
            List<MetricAggregate> batch = reportDao.fetchAggregatesOptimized(
                    tenantId,
                    groupId,
                    lastSeenId,
                    batchSize
            );

            if (batch.isEmpty()) {
                // No more data - we've processed everything
                log.debug("Batch {} returned empty - processing complete", batchNumber);
                break;
            }

            // Accumulate results
            allResults.addAll(batch);
            totalRecords += batch.size();

            // Update cursor for next batch
            // CRITICAL: Use the last item_id from this batch as the cursor
            lastSeenId = batch.get(batch.size() - 1).getItemId();

            log.debug("Batch {} processed: {} records, lastSeenId={}",
                    batchNumber, batch.size(), lastSeenId);

            // If we got fewer than batchSize, we've reached the end
            if (batch.size() < batchSize) {
                log.debug("Partial batch received - processing complete");
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Report generation complete: {} batches, {} records, {}ms",
                batchNumber, totalRecords, duration);

        return allResults;
    }

    /**
     * Generate a report with streaming callback.
     *
     * This method is preferred for very large groups where holding
     * all data in memory is not practical. The callback is invoked for
     * each batch, allowing immediate processing (e.g., writing to file,
     * streaming to client, aggregating summaries).
     *
     * MEMORY EFFICIENCY:
     * - Only one batch is held in memory at a time
     * - Suitable for groups with 100K+ items
     * - Enables progress reporting to users
     *
     * Example usage:
     * <pre>
     * reportService.generateReportWithCallback(1001L, "G001", batch -> {
     *     // Process each batch immediately
     *     batch.forEach(record -> csvWriter.write(record));
     * });
     * </pre>
     *
     * @param tenantId the tenant ID
     * @param groupId the group ID
     * @param batchCallback consumer function called for each batch
     * @return total number of records processed
     */
    public long generateReportWithCallback(
            Long tenantId,
            String groupId,
            Consumer<List<MetricAggregate>> batchCallback) {

        return generateReportWithCallback(
                tenantId, groupId, DEFAULT_BATCH_SIZE, batchCallback);
    }

    /**
     * Generate a report with streaming callback and custom batch size.
     *
     * @param tenantId the tenant ID
     * @param groupId the group ID
     * @param batchSize number of items to process per batch
     * @param batchCallback consumer function called for each batch
     * @return total number of records processed
     */
    public long generateReportWithCallback(
            Long tenantId,
            String groupId,
            int batchSize,
            Consumer<List<MetricAggregate>> batchCallback) {

        log.info("Starting streaming report for tenant={}, group={}",
                tenantId, groupId);

        String lastSeenId = "";
        int batchNumber = 0;
        long totalRecords = 0;

        long startTime = System.currentTimeMillis();

        while (true) {
            batchNumber++;

            // Fetch next batch using keyset pagination
            List<MetricAggregate> batch = reportDao.fetchAggregatesOptimized(
                    tenantId,
                    groupId,
                    lastSeenId,
                    batchSize
            );

            if (batch.isEmpty()) {
                break;
            }

            // Invoke callback to process this batch
            batchCallback.accept(batch);

            totalRecords += batch.size();
            lastSeenId = batch.get(batch.size() - 1).getItemId();

            log.debug("Streamed batch {}: {} records", batchNumber, batch.size());

            if (batch.size() < batchSize) {
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Streaming report complete: {} batches, {} records, {}ms",
                batchNumber, totalRecords, duration);

        return totalRecords;
    }

    /**
     * Estimate the number of batches needed for a group.
     *
     * Useful for progress bars and time estimation in UI.
     *
     * @param groupId the group ID
     * @param batchSize the batch size that will be used
     * @return estimated number of batches
     */
    public long estimateBatchCount(String groupId, int batchSize) {
        long itemCount = reportDao.getItemCount(groupId);
        return (itemCount + batchSize - 1) / batchSize;  // Ceiling division
    }

    /**
     * Estimate the number of batches using default batch size.
     *
     * @param groupId the compliance framework ID
     * @return estimated number of batches
     */
    public long estimateBatchCount(String groupId) {
        return estimateBatchCount(groupId, DEFAULT_BATCH_SIZE);
    }
}
