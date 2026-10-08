-- Ordering module support.
-- 1) Optimistic locking on orders: two tills paying / editing the same order cannot both win.
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 2) Keep the line discount the cashier entered, so totals can be recomputed when the order changes.
--    order_items.discount_amount stays "line discount + share of order discount" in rupees.
ALTER TABLE order_items
    ADD COLUMN discount_type VARCHAR(10) CHECK (discount_type IN ('PERCENT', 'FLAT')),
    ADD COLUMN discount_value NUMERIC(18, 2);
