-- =============================================================================
-- SCHEMA DEFINITION
-- Representative example of production tables involved in the performance issue
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: entity_catalog
-- Stores mapping of items to groups
-- In production: ~500K to 2M rows depending on client
-- -----------------------------------------------------------------------------
CREATE TABLE entity_catalog (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id         VARCHAR(50) NOT NULL,
    group_id        VARCHAR(50) NOT NULL,
    item_name       VARCHAR(255),
    description     TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Composite index optimized for the pagination query
    INDEX idx_entity_catalog_group_item (group_id, item_id),
    INDEX idx_entity_catalog_item (item_id)
);

-- -----------------------------------------------------------------------------
-- Table: entity_event
-- Stores status for each source-item combination
-- In production: ~10M to 100M+ rows (this is where the performance issue hits)
-- -----------------------------------------------------------------------------
CREATE TABLE entity_event (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_id       BIGINT NOT NULL,
    item_id         VARCHAR(50) NOT NULL,
    dimension_id    VARCHAR(50) NOT NULL,
    tenant_id       BIGINT NOT NULL,
    status          TINYINT NOT NULL DEFAULT 0,  -- 0 = failed, 1 = passed
    last_seen_time  TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Critical indexes for the reporting query
    -- This composite index is KEY to the optimization
    INDEX idx_entity_event_tenant_item (tenant_id, item_id),
    INDEX idx_entity_event_item_dimension (item_id, dimension_id),
    INDEX idx_entity_event_tenant (tenant_id)
);

-- -----------------------------------------------------------------------------
-- Table: dimension_def
-- Reference table for dimensions
-- -----------------------------------------------------------------------------
CREATE TABLE dimension_def (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(50),
    vendor          VARCHAR(100),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- Table: group_def
-- Reference table for groups
-- -----------------------------------------------------------------------------
CREATE TABLE group_def (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    version         VARCHAR(20),
    description     TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- INDEX ANALYSIS NOTES
-- =============================================================================
-- 
-- The KEY insight for this optimization:
-- 
-- Original query pattern:
--   WHERE tenant_id = ? AND item_id IN (SELECT ...)
-- 
-- This requires:
--   1. Full evaluation of the subquery
--   2. Matching against potentially millions of entity_event rows
--   3. No efficient index usage possible with IN subquery
-- 
-- Optimized query pattern:
--   JOIN (...) c ON c.item_id = e.item_id
--   WHERE e.tenant_id = ?
-- 
-- This allows:
--   1. Index scan on entity_catalog (group_id, item_id)
--   2. Index nested loop join on entity_event (tenant_id, item_id)
--   3. Much more efficient execution plan
-- 
-- =============================================================================
