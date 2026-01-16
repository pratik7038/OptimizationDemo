package com.pratik.optimizationDemo.performance.dao;

import com.pratik.optimizationDemo.performance.model.MetricAggregate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data Access Object for compliance reporting queries.
 *
 * This class demonstrates the BEFORE and AFTER of a critical SQL performance optimization.
 *
 * CONTEXT:
 * - Production table entity_event contains 50M+ rows
 * - Reports aggregate pass/fail counts per item-dimension combination
 * - Original query caused 30+ second execution times and frequent timeouts
 * - Optimized query reduced execution to ~1 second
 *
 * NOTE: This is a representative example. Table names and fields have been
 * generalized to preserve client confidentiality.
 */
@Repository
public class ReportDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<MetricAggregate> ROW_MAPPER = (rs, rowNum) ->
        new MetricAggregate(
            rs.getString("item_id"),
            rs.getString("dimension_id"),
            rs.getLong("passed"),
            rs.getLong("failed"),
            rs.getLong("error")
        );

    public ReportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================================
    // SLOW QUERY - Original Implementation (DO NOT USE IN PRODUCTION)
    // =========================================================================
    /**
     * DEPRECATED: This method demonstrates the PROBLEMATIC query pattern.
     * 
     * WHY IT'S SLOW:
     * 1. IN subquery prevents efficient join optimization
     * 2. Database materializes subquery results, then scans entity_event for matches
     * 3. With 50M rows, this becomes O(n * m) complexity
     * 4. Index on (tenant_id, item_id) cannot be used effectively
     * 
     * SYMPTOMS OBSERVED:
     * - Execution time: 30-45 seconds
     * - High CPU usage on database server
     * - Connection pool exhaustion during peak hours
     * - Timeout errors (30s threshold exceeded)
     * 
     * This method is retained for demonstration and comparison purposes only.
     * 
     * @deprecated Use {@link #fetchAggregatesOptimized} instead
     */
    @Deprecated
    public List<MetricAggregate> fetchAggregatesSlow(
            Long tenantId,
            String groupId,
            int offset,
            int limit) {

        // BAD PATTERN: IN subquery + OFFSET pagination
        // The optimizer cannot efficiently plan this query and performance
        // degrades as OFFSET grows.
        String sql = """
            SELECT item_id, dimension_id,
                   COUNT(CASE WHEN status = 1 THEN 1 END) AS passed,
                   COUNT(CASE WHEN status = 0 THEN 1 END) AS failed,
                   COUNT(CASE WHEN status = 2 THEN 1 END) AS error
            FROM entity_event
            WHERE tenant_id = ?
              AND item_id IN (
                  SELECT DISTINCT item_id
                  FROM entity_catalog
                  WHERE group_id = ?
                  ORDER BY item_id
                  OFFSET ? ROWS
                  FETCH NEXT ? ROWS ONLY
              )
            GROUP BY item_id, dimension_id
            ORDER BY item_id
            """;

        return jdbcTemplate.query(sql, ROW_MAPPER,
            tenantId, groupId, offset, limit);
    }

    // =========================================================================
    // OPTIMIZED QUERY - Fixed Implementation
    // =========================================================================
    /**
     * Retrieves aggregated posture statistics using an OPTIMIZED query pattern.
     * 
     * KEY OPTIMIZATIONS:
     * 1. Replaced IN subquery with explicit JOIN
     *    - Allows optimizer to use index nested loop join
     *    - Derived table is materialized ONCE, not per-row
     * 
     * 2. GROUP BY in derived table
     *    - Ensures unique item_ids before joining
     *    - Reduces join cardinality
     * 
     * 3. Keyset pagination (lastSeenId)
     *    - Uses index to jump directly to starting point
     *    - O(1) seek vs O(n) scan with OFFSET
     *    - Performance is constant regardless of position in dataset
     * 
     * EXECUTION PLAN IMPROVEMENT:
     * - Before: Full table scan on entity_event (50M rows)
     * - After: Index lookup for each item (1000 lookups max)
     * - Complexity: O(n * m) → O(m * log(n))
     * 
     * RESULTS:
     * - Execution time: 0.8-1.5 seconds (was 30-45 seconds)
     * - 96% reduction in query time
     * - Zero timeout errors after deployment
     * 
     * @param tenantId the tenant ID to filter by
     * @param groupId the group ID
     * @param lastSeenId the last item_id from previous batch (for pagination)
     * @param limit maximum number of items to process in this batch
     * @return list of aggregated statistics per item-dimension combination
     */
    public List<MetricAggregate> fetchAggregatesOptimized(
            Long tenantId,
            String groupId,
            String lastSeenId,
            int limit) {

        // GOOD PATTERN: JOIN with derived table
        // The optimizer can now use index nested loop join efficiently
        String sql = """
            SELECT e.item_id, e.dimension_id,
                   COUNT(CASE WHEN e.status = 1 THEN 1 END) AS passed,
                   COUNT(CASE WHEN e.status = 0 THEN 1 END) AS failed,
                   COUNT(CASE WHEN e.status = 2 THEN 1 END) AS error
            FROM entity_event e
            JOIN (
                SELECT item_id
                FROM entity_catalog
                WHERE group_id = ?
                  AND item_id > ?
                GROUP BY item_id
                ORDER BY item_id
                FETCH FIRST ? ROWS ONLY
            ) c ON c.item_id = e.item_id
            WHERE e.tenant_id = ?
            GROUP BY e.item_id, e.dimension_id
            ORDER BY e.item_id
            """;

        return jdbcTemplate.query(sql, ROW_MAPPER,
            groupId, lastSeenId, limit, tenantId);
    }

    public long getItemCount(String groupId) {
        String sql = """
            SELECT COUNT(DISTINCT item_id)
            FROM entity_catalog
            WHERE group_id = ?
            """;

        Long count = jdbcTemplate.queryForObject(sql, Long.class, groupId);
        return count != null ? count : 0L;
    }
}
