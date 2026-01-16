-- =============================================================================
-- OPTIMIZED QUERY (Fixed Implementation)
-- =============================================================================

-- Parameters:
-- :tenantId = 1001
-- :groupId = 'G001'
-- :lastSeenId = '' (empty string or last item_id from previous batch)
-- :limit = 1000

SELECT 
    e.item_id, 
    e.dimension_id,
    COUNT(CASE WHEN e.status = 1 THEN 1 END) AS passed,
    COUNT(CASE WHEN e.status = 0 THEN 1 END) AS failed
FROM entity_event e
JOIN (
    SELECT item_id
    FROM entity_catalog
    WHERE group_id = :groupId
      AND item_id > :lastSeenId
    GROUP BY item_id
    ORDER BY item_id
    FETCH FIRST :limit ROWS ONLY
) c ON c.item_id = e.item_id
WHERE e.tenant_id = :tenantId
GROUP BY e.item_id, e.dimension_id
ORDER BY e.item_id;
