-- =============================================================================
-- SLOW QUERY (Original Implementation)
-- =============================================================================
-- 
-- This query represents the PROBLEMATIC pattern that was causing timeouts.
-- 
-- =============================================================================

-- Parameters:
-- :tenantId = 1001
-- :groupId = 'G001'
-- :lastSeenId = '' (empty string or last item_id from previous batch)
-- :limit = 1000

SELECT 
    item_id, 
    dimension_id,
    COUNT(CASE WHEN status = 1 THEN 1 END) AS passed,
    COUNT(CASE WHEN status = 0 THEN 1 END) AS failed
FROM entity_event
WHERE tenant_id = :tenantId
  AND item_id IN (
      SELECT item_id
      FROM entity_catalog
      WHERE group_id = :groupId
        AND item_id > :lastSeenId
      ORDER BY item_id
      FETCH FIRST :limit ROWS ONLY
  )
GROUP BY item_id, dimension_id
ORDER BY item_id;
