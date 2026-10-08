-- ---------------------------------------------------------------------
-- SHIFTS / CASH DRAWER
-- A shift runs from "open" (count the opening cash) to "close" (count the cash again).
-- Expected cash = opening + cash payments - cash refunds (during the shift) + cash in - cash out.
-- One open shift per shop at a time (single cash drawer).
-- ---------------------------------------------------------------------
CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    opened_by VARCHAR(36),
    opened_at TIMESTAMP NOT NULL,
    opening_cash NUMERIC(18, 2) NOT NULL CHECK (opening_cash >= 0),
    opening_notes TEXT,
    -- filled when the shift is closed
    closed_by VARCHAR(36),
    closed_at TIMESTAMP,
    cash_sales NUMERIC(18, 2),                              -- cash payments - cash refunds during the shift
    cash_in NUMERIC(18, 2),
    cash_out NUMERIC(18, 2),
    expected_cash NUMERIC(18, 2),
    counted_cash NUMERIC(18, 2) CHECK (counted_cash IS NULL OR counted_cash >= 0),
    cash_difference NUMERIC(18, 2),                         -- counted - expected (negative = short)
    closing_notes TEXT
);
CREATE UNIQUE INDEX uq_shifts_one_open ON shifts (tenant_uuid) WHERE status = 'OPEN' AND is_deleted = false;
CREATE INDEX idx_shifts_tenant_opened ON shifts (tenant_uuid, opened_at DESC) WHERE is_deleted = false;

-- Cash put into (float, change) or taken out of (supplier paid, bank deposit) the drawer during a shift.
CREATE TABLE cash_movements (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    shift_id BIGINT NOT NULL REFERENCES shifts (id),
    movement_type VARCHAR(10) NOT NULL CHECK (movement_type IN ('IN', 'OUT')),
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(255) NOT NULL,
    created_by VARCHAR(36),
    moved_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_cash_movements_shift ON cash_movements (shift_id) WHERE is_deleted = false;
