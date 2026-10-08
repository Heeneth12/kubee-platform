-- ---------------------------------------------------------------------
-- ONLINE ORDERS (phase 1: entered by hand)
-- The shop accepts a Zomato / Swiggy order on the partner app and enters it at the POS.
-- orders.source says where the order came from; external_order_id is the aggregator's order id.
-- The customer already paid the aggregator, so these orders are paid with payment_method = 'AGGREGATOR'.
-- ---------------------------------------------------------------------
ALTER TABLE orders
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'POS'
        CHECK (source IN ('POS', 'ZOMATO', 'SWIGGY', 'OTHER')),
    ADD COLUMN external_order_id VARCHAR(64);

-- Look-up when entering an order (stop duplicates) and searching by the aggregator's order id.
CREATE INDEX idx_orders_external_order_id ON orders (tenant_uuid, external_order_id)
    WHERE external_order_id IS NOT NULL;

-- Allow the new payment method. V1 declared the check inline, so find it by its definition, not its name.
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'payments' AND rel.relnamespace = current_schema()::regnamespace
          AND con.contype = 'c' AND pg_get_constraintdef(con.oid) LIKE '%payment_method%'
    LOOP
        EXECUTE format('ALTER TABLE payments DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('CASH', 'UPI', 'CARD', 'WALLET', 'BANK_TRANSFER', 'OTHER', 'AGGREGATOR'));
