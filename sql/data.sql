-- =============================================================================
-- SAMPLE DATA
-- Representative data for demonstrating the performance difference
-- In production, entity_event would have millions of rows
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Reference Data: Frameworks
-- -----------------------------------------------------------------------------
INSERT INTO group_def (id, name, version, description) VALUES
('G001', 'Group A', '1.0', 'Sample group definition A'),
('G002', 'Group B', '1.0', 'Sample group definition B'),
('G003', 'Group C', '1.0', 'Sample group definition C');

-- -----------------------------------------------------------------------------
-- Reference Data: Technologies
-- -----------------------------------------------------------------------------
INSERT INTO dimension_def (id, name, category, vendor) VALUES
('D001', 'Dimension 1', 'Type A', 'Vendor X'),
('D002', 'Dimension 2', 'Type A', 'Vendor X'),
('D003', 'Dimension 3', 'Type B', 'Vendor Y'),
('D004', 'Dimension 4', 'Type B', 'Vendor Y');

-- -----------------------------------------------------------------------------
-- Catalog: Mapping items to groups
-- -----------------------------------------------------------------------------
INSERT INTO entity_catalog (item_id, group_id, item_name, description) VALUES
('I-001', 'G001', 'Item 001', 'Sample item for group A'),
('I-002', 'G001', 'Item 002', 'Sample item for group A'),
('I-003', 'G001', 'Item 003', 'Sample item for group A'),
('I-004', 'G001', 'Item 004', 'Sample item for group A'),
('I-005', 'G001', 'Item 005', 'Sample item for group A'),
('I-006', 'G001', 'Item 006', 'Sample item for group A'),
('I-007', 'G001', 'Item 007', 'Sample item for group A'),
('I-008', 'G001', 'Item 008', 'Sample item for group A'),
('I-009', 'G001', 'Item 009', 'Sample item for group A'),
('I-010', 'G001', 'Item 010', 'Sample item for group A'),
('I-011', 'G002', 'Item 011', 'Sample item for group B'),
('I-012', 'G002', 'Item 012', 'Sample item for group B'),
('I-013', 'G002', 'Item 013', 'Sample item for group B'),
('I-014', 'G002', 'Item 014', 'Sample item for group B'),
('I-015', 'G002', 'Item 015', 'Sample item for group B'),
('I-016', 'G003', 'Item 016', 'Sample item for group C'),
('I-017', 'G003', 'Item 017', 'Sample item for group C'),
('I-018', 'G003', 'Item 018', 'Sample item for group C'),
('I-019', 'G003', 'Item 019', 'Sample item for group C'),
('I-020', 'G003', 'Item 020', 'Sample item for group C');

-- -----------------------------------------------------------------------------
-- Event Data: Status results
-- -----------------------------------------------------------------------------

-- Tenant 1001
INSERT INTO entity_event (source_id, item_id, dimension_id, tenant_id, status, last_seen_time)
SELECT 
    h.host_num,
    c.item_id,
    d.id,
    1001,
    FLOOR(RAND() * 2),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 24) HOUR)
FROM 
    (SELECT 1 AS host_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
     UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20) h,
    (SELECT item_id FROM entity_catalog WHERE group_id = 'G001') c,
    (SELECT id FROM dimension_def WHERE id IN ('D001', 'D002', 'D003')) d;

-- Tenant 1002
INSERT INTO entity_event (source_id, item_id, dimension_id, tenant_id, status, last_seen_time)
SELECT 
    h.host_num + 100,
    c.item_id,
    d.id,
    1002,
    FLOOR(RAND() * 2),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 48) HOUR)
FROM 
    (SELECT 1 AS host_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) h,
    (SELECT item_id FROM entity_catalog WHERE group_id = 'G002') c,
    (SELECT id FROM dimension_def WHERE id IN ('D002', 'D003', 'D004')) d;

-- Tenant 1003
INSERT INTO entity_event (source_id, item_id, dimension_id, tenant_id, status, last_seen_time)
SELECT 
    h.host_num + 200,
    c.item_id,
    d.id,
    1003,
    FLOOR(RAND() * 2),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 72) HOUR)
FROM 
    (SELECT 1 AS host_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) h,
    (SELECT item_id FROM entity_catalog WHERE group_id = 'G003') c,
    (SELECT id FROM dimension_def WHERE id IN ('D001', 'D004')) d;

-- =============================================================================
-- DATA VOLUME NOTES
-- =============================================================================
-- 
-- This sample data creates a small dataset for demonstration.
-- In the actual production environment where this issue was discovered:
-- 
-- - entity_catalog: ~500,000 rows
-- - entity_event: ~50,000,000 rows
-- - Queries were timing out after 30+ seconds
-- 
-- =============================================================================
