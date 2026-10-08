-- Reports filter completed / cancelled orders by date.
CREATE INDEX idx_orders_tenant_completed ON orders (tenant_uuid, completed_at)
    WHERE status = 'COMPLETED' AND is_deleted = false;
CREATE INDEX idx_orders_tenant_cancelled ON orders (tenant_uuid, cancelled_at)
    WHERE status = 'CANCELLED' AND is_deleted = false;
