-- =====================================================================
-- Kubee POS - core schema (catalog, taxes, orders, billing, payments)
-- No inventory: items are just a sellable price list.
--
-- Flow:  item (Veg Maggi) -> order (cart) -> bill (invoice) -> payment
--        Payment can happen before OR after the bill is generated,
--        so payments belong to the ORDER, not to the bill.
--
--   tax_groups 1--* tax_components
--   categories 1--* items 1--* item_variants
--   addon_groups 1--* addons ; items *--* addon_groups (item_addon_groups)
--   orders 1--* order_items 1--* order_item_addons
--   orders 1--* payments
--   orders 1--* bills (only one ISSUED at a time) 1--* bill_items 1--* bill_item_taxes
--
-- Conventions (same as ezinventory):
--   id BIGSERIAL internal PK / FK, uuid exposed in APIs,
--   tenant_uuid from the auth service on every table,
--   soft delete via is_deleted, money NUMERIC(18,2), qty NUMERIC(12,3).
--   Names / prices / taxes are SNAPSHOTTED on order & bill lines so old
--   bills never change when the catalog changes.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS pos;
SET search_path TO pos;

-- ---------------------------------------------------------------------
-- SETTINGS (one row per tenant / shop)
-- ---------------------------------------------------------------------
CREATE TABLE pos_settings (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    shop_name VARCHAR(255) NOT NULL,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(255),
    gstin VARCHAR(15),
    state_code VARCHAR(2),                                  -- GST state code, e.g. '36' Telangana
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    prices_include_tax BOOLEAN NOT NULL DEFAULT true,       -- default for new items
    round_off_mode VARCHAR(20) NOT NULL DEFAULT 'NEAREST_1'
        CHECK (round_off_mode IN ('NONE', 'NEAREST_1', 'NEAREST_0_50', 'DOWN_1')),
    bill_prefix VARCHAR(20),
    bill_header TEXT,
    bill_footer TEXT,
    logo_url VARCHAR(500),
    financial_year_start_month SMALLINT NOT NULL DEFAULT 4 CHECK (financial_year_start_month BETWEEN 1 AND 12)
);

-- Running numbers for order tokens and bill numbers.
-- series_code lets each counter/device own a range so offline billing never collides.
-- period_key: '2025-26' for FY-wise bill numbers, '2025-10-04' for daily order tokens.
CREATE TABLE number_sequences (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    sequence_type VARCHAR(20) NOT NULL CHECK (sequence_type IN ('ORDER', 'BILL')),
    series_code VARCHAR(20) NOT NULL DEFAULT 'A',
    period_key VARCHAR(20) NOT NULL,
    prefix VARCHAR(30),
    current_value BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_uuid, sequence_type, series_code, period_key)
);

-- ---------------------------------------------------------------------
-- TAXES
-- tax_group = what an item points to ("GST 5%")
-- tax_component = how it splits on the bill (CGST 2.5% + SGST 2.5%)
-- ---------------------------------------------------------------------
CREATE TABLE tax_groups (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    name VARCHAR(100) NOT NULL,                             -- 'GST 5%', 'No Tax'
    total_rate NUMERIC(6, 3) NOT NULL DEFAULT 0 CHECK (total_rate >= 0),
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_tax_groups_name ON tax_groups (tenant_uuid, lower(name)) WHERE is_deleted = false;
CREATE UNIQUE INDEX uq_tax_groups_default ON tax_groups (tenant_uuid) WHERE is_default = true AND is_deleted = false;

CREATE TABLE tax_components (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    tax_group_id BIGINT NOT NULL REFERENCES tax_groups (id),
    name VARCHAR(50) NOT NULL,                              -- 'CGST', 'SGST', 'IGST', 'CESS'
    tax_type VARCHAR(20) NOT NULL CHECK (tax_type IN ('CGST', 'SGST', 'IGST', 'CESS', 'OTHER')),
    rate NUMERIC(6, 3) NOT NULL CHECK (rate >= 0),
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_tax_components_group ON tax_components (tax_group_id);

-- ---------------------------------------------------------------------
-- CATALOG
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    name VARCHAR(255) NOT NULL,                             -- 'Maggi', 'Beverages'
    parent_id BIGINT REFERENCES categories (id),
    image_url VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_categories_name ON categories (tenant_uuid, COALESCE(parent_id, 0), lower(name)) WHERE is_deleted = false;

CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    name VARCHAR(255) NOT NULL,                             -- 'Veg Maggi'
    short_name VARCHAR(50),                                 -- printed on small thermal bills
    item_code VARCHAR(50),                                  -- quick-type code, e.g. '101'
    barcode VARCHAR(100),
    category_id BIGINT REFERENCES categories (id),
    item_type VARCHAR(20) NOT NULL DEFAULT 'GOODS' CHECK (item_type IN ('GOODS', 'SERVICE')),
    food_type VARCHAR(10) CHECK (food_type IN ('VEG', 'NON_VEG', 'EGG')),   -- NULL for non-food shops
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PCS',     -- PCS, PLATE, KG, LTR ...
    selling_price NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (selling_price >= 0),
    mrp NUMERIC(18, 2),
    price_includes_tax BOOLEAN NOT NULL DEFAULT true,
    tax_group_id BIGINT REFERENCES tax_groups (id),         -- NULL = no tax
    hsn_sac_code VARCHAR(20),
    has_variants BOOLEAN NOT NULL DEFAULT false,            -- true => must pick a variant when billing
    is_open_price BOOLEAN NOT NULL DEFAULT false,           -- cashier types the price at billing time
    is_favourite BOOLEAN NOT NULL DEFAULT false,            -- shows as a quick button on billing screen
    image_url VARCHAR(500),
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_items_code ON items (tenant_uuid, item_code) WHERE item_code IS NOT NULL AND is_deleted = false;
CREATE UNIQUE INDEX uq_items_barcode ON items (tenant_uuid, barcode) WHERE barcode IS NOT NULL AND is_deleted = false;
CREATE INDEX idx_items_tenant_category ON items (tenant_uuid, category_id) WHERE is_deleted = false;
CREATE INDEX idx_items_tenant_name ON items (tenant_uuid, lower(name)) WHERE is_deleted = false;

-- Half / Full, Small / Large, 250g / 500g - each with its own price.
CREATE TABLE item_variants (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    item_id BIGINT NOT NULL REFERENCES items (id),
    name VARCHAR(100) NOT NULL,
    item_code VARCHAR(50),
    barcode VARCHAR(100),
    selling_price NUMERIC(18, 2) NOT NULL CHECK (selling_price >= 0),
    mrp NUMERIC(18, 2),
    is_default BOOLEAN NOT NULL DEFAULT false,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX idx_item_variants_item ON item_variants (item_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX uq_item_variants_barcode ON item_variants (tenant_uuid, barcode) WHERE barcode IS NOT NULL AND is_deleted = false;
CREATE UNIQUE INDEX uq_item_variants_code ON item_variants (tenant_uuid, item_code) WHERE item_code IS NOT NULL AND is_deleted = false;

-- Add-ons: 'Extra Cheese', 'Extra Butter'. Grouped so we can say "pick up to 2".
CREATE TABLE addon_groups (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    name VARCHAR(100) NOT NULL,                             -- 'Toppings'
    min_select INT NOT NULL DEFAULT 0 CHECK (min_select >= 0),
    max_select INT CHECK (max_select IS NULL OR max_select >= min_select),   -- NULL = no limit
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE addons (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    addon_group_id BIGINT NOT NULL REFERENCES addon_groups (id),
    name VARCHAR(100) NOT NULL,
    price NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (price >= 0),
    food_type VARCHAR(10) CHECK (food_type IN ('VEG', 'NON_VEG', 'EGG')),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX idx_addons_group ON addons (addon_group_id) WHERE is_deleted = false;

CREATE TABLE item_addon_groups (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    item_id BIGINT NOT NULL REFERENCES items (id),
    addon_group_id BIGINT NOT NULL REFERENCES addon_groups (id),
    sort_order INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_item_addon_groups ON item_addon_groups (item_id, addon_group_id) WHERE is_deleted = false;

-- ---------------------------------------------------------------------
-- ORDERS  (the cart: what the customer asked for)
-- status:          OPEN -> (HELD <-> OPEN) -> COMPLETED | CANCELLED
-- payment_status:  UNPAID -> PARTIALLY_PAID -> PAID   (REFUNDED / PARTIALLY_REFUNDED)
-- Totals are kept up to date by the app on every change.
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    client_ref VARCHAR(64),                                 -- idempotency key from device (offline sync)
    order_number VARCHAR(50) NOT NULL,                      -- token shown to customer, e.g. '23'
    order_type VARCHAR(20) NOT NULL DEFAULT 'COUNTER'
        CHECK (order_type IN ('COUNTER', 'TAKEAWAY', 'DINE_IN', 'DELIVERY')),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'HELD', 'COMPLETED', 'CANCELLED')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
        CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    customer_name VARCHAR(255),
    customer_phone VARCHAR(20),
    table_label VARCHAR(50),                                -- optional, for dine-in later
    notes TEXT,
    sub_total NUMERIC(18, 2) NOT NULL DEFAULT 0,            -- sum of line amounts before order discount
    discount_type VARCHAR(10) CHECK (discount_type IN ('PERCENT', 'FLAT')),
    discount_value NUMERIC(18, 2),
    discount_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,      -- line + order discounts, in rupees
    discount_reason VARCHAR(255),
    taxable_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    round_off_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,          -- net of refunds
    created_by VARCHAR(36),                                 -- user uuid from auth
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(36),
    cancel_reason VARCHAR(255)
);
CREATE UNIQUE INDEX uq_orders_client_ref ON orders (tenant_uuid, client_ref) WHERE client_ref IS NOT NULL;
CREATE INDEX idx_orders_tenant_created ON orders (tenant_uuid, created_at DESC) WHERE is_deleted = false;
CREATE INDEX idx_orders_tenant_status ON orders (tenant_uuid, status) WHERE is_deleted = false;
CREATE INDEX idx_orders_tenant_phone ON orders (tenant_uuid, customer_phone) WHERE customer_phone IS NOT NULL;

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    item_id BIGINT REFERENCES items (id),                   -- NULL allowed for a one-off custom line
    item_variant_id BIGINT REFERENCES item_variants (id),
    -- snapshots at the moment the item was added
    item_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(100),
    hsn_sac_code VARCHAR(20),
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PCS',
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(18, 2) NOT NULL CHECK (unit_price >= 0),     -- item/variant price
    addons_unit_price NUMERIC(18, 2) NOT NULL DEFAULT 0,            -- sum of add-ons per unit
    price_includes_tax BOOLEAN NOT NULL DEFAULT true,
    line_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,                  -- (unit_price + addons) * qty
    discount_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,              -- line discount + share of order discount
    tax_group_id BIGINT REFERENCES tax_groups (id),
    tax_rate NUMERIC(6, 3) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,                 -- taxable + tax
    notes VARCHAR(255),                                             -- 'less spicy'
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_order_items_order ON order_items (order_id) WHERE is_deleted = false;
CREATE INDEX idx_order_items_tenant_item ON order_items (tenant_uuid, item_id);

CREATE TABLE order_item_addons (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    order_item_id BIGINT NOT NULL REFERENCES order_items (id),
    addon_id BIGINT REFERENCES addons (id),
    addon_name VARCHAR(100) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL DEFAULT 1 CHECK (quantity > 0),   -- per unit of the parent line
    unit_price NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0)
);
CREATE INDEX idx_order_item_addons_line ON order_item_addons (order_item_id) WHERE is_deleted = false;

-- ---------------------------------------------------------------------
-- PAYMENTS  (attached to the ORDER so they work before or after billing)
-- One order can have many payments => split payment (cash + UPI).
-- Refunds are rows with payment_type = 'REFUND'.
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    client_ref VARCHAR(64),
    order_id BIGINT NOT NULL REFERENCES orders (id),
    payment_type VARCHAR(10) NOT NULL DEFAULT 'PAYMENT' CHECK (payment_type IN ('PAYMENT', 'REFUND')),
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('CASH', 'UPI', 'CARD', 'WALLET', 'BANK_TRANSFER', 'OTHER')),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),   -- PENDING = waiting for UPI confirmation
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    tendered_amount NUMERIC(18, 2),                         -- cash given by customer
    change_amount NUMERIC(18, 2),                           -- cash returned
    reference_no VARCHAR(100),                              -- UPI UTR / card approval code
    gateway VARCHAR(30),                                    -- RAZORPAY, PHONEPE ... NULL for manual
    gateway_payment_id VARCHAR(100),
    gateway_response JSONB,
    refund_of_payment_id BIGINT REFERENCES payments (id),
    notes VARCHAR(255),
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_by VARCHAR(36)
);
CREATE UNIQUE INDEX uq_payments_client_ref ON payments (tenant_uuid, client_ref) WHERE client_ref IS NOT NULL;
CREATE UNIQUE INDEX uq_payments_gateway_id ON payments (gateway, gateway_payment_id) WHERE gateway_payment_id IS NOT NULL;
CREATE INDEX idx_payments_order ON payments (order_id) WHERE is_deleted = false;
CREATE INDEX idx_payments_tenant_paid_at ON payments (tenant_uuid, paid_at DESC) WHERE is_deleted = false;

-- ---------------------------------------------------------------------
-- BILLS  (the GST invoice generated from an order)
-- Immutable once ISSUED. To change it: CANCEL it and issue a new one.
-- Only one ISSUED bill per order at a time. Payment status lives on the order.
-- ---------------------------------------------------------------------
CREATE TABLE bills (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    bill_number VARCHAR(50) NOT NULL,                       -- 'A/2025-26/000123'
    bill_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED', 'CANCELLED')),
    -- seller snapshot (GST invoice must not change if settings change later)
    seller_name VARCHAR(255) NOT NULL,
    seller_address TEXT,
    seller_gstin VARCHAR(15),
    seller_state_code VARCHAR(2),
    -- buyer (optional; GSTIN only for B2B bills)
    customer_name VARCHAR(255),
    customer_phone VARCHAR(20),
    customer_gstin VARCHAR(15),
    place_of_supply VARCHAR(2),                             -- state code; differs from seller => IGST
    -- totals (copied from order at issue time)
    sub_total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    round_off_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    print_count INT NOT NULL DEFAULT 0,
    shared_via VARCHAR(20),                                 -- PRINT, WHATSAPP, SMS
    pdf_url VARCHAR(500),
    issued_by VARCHAR(36),
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(36),
    cancel_reason VARCHAR(255)
);
CREATE UNIQUE INDEX uq_bills_number ON bills (tenant_uuid, bill_number);
CREATE UNIQUE INDEX uq_bills_one_issued_per_order ON bills (order_id) WHERE status = 'ISSUED' AND is_deleted = false;
CREATE INDEX idx_bills_tenant_date ON bills (tenant_uuid, bill_date DESC) WHERE is_deleted = false;

CREATE TABLE bill_items (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    bill_id BIGINT NOT NULL REFERENCES bills (id),
    order_item_id BIGINT REFERENCES order_items (id),
    item_id BIGINT REFERENCES items (id),
    item_name VARCHAR(255) NOT NULL,                        -- 'Veg Maggi (Full)'
    addons_text VARCHAR(500),                               -- '+ Extra Cheese'
    hsn_sac_code VARCHAR(20),
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'PCS',
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(18, 2) NOT NULL,                     -- including add-ons
    line_amount NUMERIC(18, 2) NOT NULL,
    discount_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_rate NUMERIC(6, 3) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_bill_items_bill ON bill_items (bill_id);

-- Per-line tax split (CGST 2.5 + SGST 2.5) -> drives the GST summary report.
CREATE TABLE bill_item_taxes (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    bill_item_id BIGINT NOT NULL REFERENCES bill_items (id),
    tax_component_id BIGINT REFERENCES tax_components (id),
    tax_type VARCHAR(20) NOT NULL CHECK (tax_type IN ('CGST', 'SGST', 'IGST', 'CESS', 'OTHER')),
    tax_name VARCHAR(50) NOT NULL,
    rate NUMERIC(6, 3) NOT NULL,
    taxable_amount NUMERIC(18, 2) NOT NULL,
    tax_amount NUMERIC(18, 2) NOT NULL
);
CREATE INDEX idx_bill_item_taxes_line ON bill_item_taxes (bill_item_id);
CREATE INDEX idx_bill_item_taxes_tenant_type ON bill_item_taxes (tenant_uuid, tax_type, rate);
