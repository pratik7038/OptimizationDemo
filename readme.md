# SQL Performance Optimization – Batch Processing & Pagination (Representative Case)

## Overview
This project demonstrates how a real-world SQL performance issue was identified
and resolved in a Spring Boot backend handling large datasets.

The original implementation caused frequent query timeouts under load.
The optimized solution significantly reduced execution time by improving
query structure and pagination strategy.

---

## Problem Description
A reporting query was using an `IN (subquery)` pattern combined with pagination.
As data volume grew, this resulted in slow execution and database timeouts.

### Symptoms Observed
- Query execution times exceeding 30+ seconds
- Database connection pool exhaustion
- Timeout errors during peak reporting hours
- Increased CPU usage on database server

---

## Root Cause Analysis

### 1. Inefficient `IN` Subquery
```sql
WHERE item_id IN (SELECT item_id FROM ... FETCH FIRST n ROWS)
```
- The database optimizer struggles to efficiently plan queries with `IN` subqueries on large datasets
- Each outer row potentially triggers a subquery evaluation
- Index utilization becomes unpredictable

### 2. Offset-Based Pagination Issues
- Traditional `OFFSET` pagination scans and discards rows
- Performance degrades linearly as offset increases
- At offset 100,000, database must scan 100,000+ rows before returning results

### 3. Missing Index Optimization
- Composite indexes not aligned with query patterns
- Full table scans on frequently filtered columns

---

## Solution Implementation

### 1. Replaced `IN` Subquery with Indexed `JOIN`
```sql
-- Before: IN subquery (slow)
WHERE item_id IN (SELECT item_id FROM entity_catalog ...)

-- After: JOIN (optimized)
JOIN (SELECT item_id FROM entity_catalog ... GROUP BY item_id) c 
  ON c.item_id = e.item_id
```

### 2. Keyset Pagination (Cursor-Based)
```sql
-- Before: Offset-based (slow at high offsets)
OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY

-- After: Keyset pagination (consistent performance)
WHERE item_id > :lastSeenId
ORDER BY item_id
FETCH FIRST :limit ROWS ONLY
```

### 3. Batch Processing Strategy
- Process data in controlled batches (e.g., 1000 records)
- Track `lastSeenId` between batches
- Prevents memory exhaustion and maintains responsiveness

---

## Performance Results

| Metric | Before | After | Improvement    |
|--------|--------|-------|----------------|
| Avg Query Time | 10s    | 1.2s | ~88% faster    |
| Timeout Rate | 15%    | <0.1% | Eliminated     |

---

## Project Structure
```
optimizationDemo/
├── readme.md
├── pom.xml
├── sql/
│   ├── schema.sql          # Table definitions with indexes
│   ├── data.sql            # Sample data for testing
│   ├── slow_query.sql      # Original problematic query
│   └── optimized_query.sql # Optimized query with JOIN
└── src/main/java/com/pratik/optimizationDemo/performance/
    ├── controller/
    │   └── PerformanceTestController.java  # Endpoints to run slow/optimized queries
    ├── dao/
    │   └── ReportDao.java      # Data access with both query versions
    ├── model/
    │   └── MetricAggregate.java  # Aggregated result model
    └── service/
        └── ReportService.java  # Batch processing logic
```

---

## Key Code Highlights

### ReportDao.java
Contains both query implementations with detailed comments explaining:
- Why the original approach was slow
- How the optimized approach fixes the issue
- Index utilization considerations

### ReportService.java
Demonstrates production-ready patterns:
- Batch processing with configurable batch size
- Keyset pagination using `lastSeenId`
- Graceful handling of large datasets

---

## How to Reproduce the Issue

1. Load sample data using `sql/data.sql`
2. Execute `sql/slow_query.sql` – observe execution time
3. Execute `sql/optimized_query.sql` – compare performance
4. Review Java implementation for programmatic approach

---

## Lessons Learned

1. **Always EXPLAIN ANALYZE** – Never assume query performance; measure it
2. **IN subqueries don't scale** – Prefer JOINs for large datasets
3. **Keyset > Offset** – For any pagination beyond small datasets
4. **Batch processing is essential** – Never fetch unbounded result sets
5. **Index alignment matters** – Design indexes for actual query patterns

---

## Notes on Confidentiality
This project is a **representative example** of a production SQL performance issue I resolved.
All table names, fields, and data have been generalized to preserve
client and employer confidentiality.

---

## Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL / MySQL compatible
- **Data Access:** Spring JDBC Template

---

## Author
Optimized and documented as part of backend performance engineering work.
Demonstrates real-world experience with:
- Query optimization at scale
- Database performance tuning
- Production incident resolution
