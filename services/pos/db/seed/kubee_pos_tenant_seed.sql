-- =====================================================================
-- Catalog seed for the real auth-service tenant "kubee pos". NOT a Flyway migration.
--
--   psql -h localhost -U venkatheenethsai -d testdb -f db/seed/kubee_pos_tenant_seed.sql
--
-- Tenant  f3d347fa-0029-4d2f-8dec-66e21cec046c  "kubee pos" (auth tenantId 3, login kubee@pos.com)
--
-- Re-runnable: wipes (incl. orders, payments, bills, shifts) and reloads ONLY this tenant's catalog + settings.
-- Run it after the app has started once (so Flyway has created the schema).
--
-- Readable uuid prefixes:  f31 settings · f32/f321 tax groups/components · f33 categories
--                          f34/f341 add-on groups/add-ons · f35 items · f351 variants · f352 item <-> add-on links
-- Tax rates and HSN codes are sample values for testing, not tax advice.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------- reset (this tenant only)
-- orders reference items, so test orders/bills/payments/shifts go first (dev data only)
DELETE FROM pos.bill_item_taxes   WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.bill_items        WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.bills             WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.payments          WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND refund_of_payment_id IS NOT NULL;
DELETE FROM pos.payments          WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.order_item_addons WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.order_items       WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.orders            WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.number_sequences  WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.cash_movements    WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.shifts            WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.item_addon_groups WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.item_variants     WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.items             WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.addons            WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.addon_groups      WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.categories        WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND parent_id IS NOT NULL;
DELETE FROM pos.categories        WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.tax_components    WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.tax_groups        WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';
DELETE FROM pos.pos_settings      WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c';

-- ---------------------------------------------------------------- shop settings
INSERT INTO pos.pos_settings (uuid, tenant_uuid, shop_name, address, phone, email, gstin, state_code, currency_code,
                              prices_include_tax, round_off_mode, bill_prefix, bill_header, bill_footer,
                              financial_year_start_month)
VALUES ('f3100000-0000-0000-0000-000000000001', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'kubee pos',
        'Shop 12, Main Road, Hyderabad', NULL, 'kubee@pos.com', NULL, '36', 'INR',
        true, 'NEAREST_1', 'KP', 'kubee pos', 'Thank you! Visit again.', 4);

-- ---------------------------------------------------------------- tax groups
INSERT INTO pos.tax_groups (uuid, tenant_uuid, name, total_rate, is_default) VALUES
    ('f3200000-0000-0000-0000-000000000001', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'No Tax',  0,  false),
    ('f3200000-0000-0000-0000-000000000002', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'GST 5%',  5,  true),
    ('f3200000-0000-0000-0000-000000000003', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'GST 12%', 12, false),
    ('f3200000-0000-0000-0000-000000000004', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'GST 18%', 18, false);

-- each GST group splits half CGST + half SGST
INSERT INTO pos.tax_components (uuid, tenant_uuid, tax_group_id, name, tax_type, rate, sort_order)
SELECT 'f3210000-0000-0000-0000-0000000000' || lpad((row_number() OVER (ORDER BY tg.id, c.ord))::text, 2, '0'),
       tg.tenant_uuid, tg.id, c.tax_type, c.tax_type, tg.total_rate / 2, c.ord
FROM pos.tax_groups tg
CROSS JOIN (VALUES ('CGST', 1), ('SGST', 2)) AS c(tax_type, ord)
WHERE tg.tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c'
  AND tg.total_rate > 0;

-- ---------------------------------------------------------------- categories (top level first)
INSERT INTO pos.categories (uuid, tenant_uuid, name, sort_order, is_active) VALUES
    ('f3300000-0000-0000-0000-000000000001', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Food',      1, true),
    ('f3300000-0000-0000-0000-000000000002', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Beverages', 2, true),
    ('f3300000-0000-0000-0000-000000000003', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Desserts',  3, true),
    ('f3300000-0000-0000-0000-000000000004', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Packaged',  4, true),
    ('f3300000-0000-0000-0000-000000000005', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Combos',    5, false);

INSERT INTO pos.categories (uuid, tenant_uuid, name, parent_id, sort_order, is_active)
SELECT v.uuid, p.tenant_uuid, v.name, p.id, v.sort_order, true
FROM (VALUES
    ('f3300000-0000-0000-0000-000000000011', 'Breakfast',   'f3300000-0000-0000-0000-000000000001', 1),
    ('f3300000-0000-0000-0000-000000000012', 'Burgers',     'f3300000-0000-0000-0000-000000000001', 2),
    ('f3300000-0000-0000-0000-000000000013', 'Pizza',       'f3300000-0000-0000-0000-000000000001', 3),
    ('f3300000-0000-0000-0000-000000000014', 'Snacks',      'f3300000-0000-0000-0000-000000000001', 4),
    ('f3300000-0000-0000-0000-000000000021', 'Hot Drinks',  'f3300000-0000-0000-0000-000000000002', 1),
    ('f3300000-0000-0000-0000-000000000022', 'Cold Drinks', 'f3300000-0000-0000-0000-000000000002', 2),
    ('f3300000-0000-0000-0000-000000000023', 'Shakes',      'f3300000-0000-0000-0000-000000000002', 3)
) AS v(uuid, name, parent_uuid, sort_order)
JOIN pos.categories p ON p.uuid = v.parent_uuid;

-- ---------------------------------------------------------------- add-on groups + add-ons
INSERT INTO pos.addon_groups (uuid, tenant_uuid, name, min_select, max_select, is_active) VALUES
    ('f3400000-0000-0000-0000-000000000001', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Extra Toppings', 0, 4, true),
    ('f3400000-0000-0000-0000-000000000002', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Spice Level',    1, 1, true),
    ('f3400000-0000-0000-0000-000000000003', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Crust',          1, 1, true),
    ('f3400000-0000-0000-0000-000000000004', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Milk Options',   0, 1, true),
    ('f3400000-0000-0000-0000-000000000005', 'f3d347fa-0029-4d2f-8dec-66e21cec046c', 'Sugar',          0, 1, true);

INSERT INTO pos.addons (uuid, tenant_uuid, addon_group_id, name, price, food_type, sort_order, is_active)
SELECT v.uuid, g.tenant_uuid, g.id, v.name, v.price::numeric, v.food_type, v.sort_order, v.active
FROM (VALUES
    ('f3410000-0000-0000-0000-000000000001', 'f3400000-0000-0000-0000-000000000001', 'Extra Cheese',   '30', 'VEG',     1, true),
    ('f3410000-0000-0000-0000-000000000002', 'f3400000-0000-0000-0000-000000000001', 'Jalapenos',      '20', 'VEG',     2, true),
    ('f3410000-0000-0000-0000-000000000003', 'f3400000-0000-0000-0000-000000000001', 'Mushrooms',      '25', 'VEG',     3, true),
    ('f3410000-0000-0000-0000-000000000004', 'f3400000-0000-0000-0000-000000000001', 'Chicken Strips', '50', 'NON_VEG', 4, true),
    ('f3410000-0000-0000-0000-000000000005', 'f3400000-0000-0000-0000-000000000001', 'Fried Egg',      '20', 'EGG',     5, true),
    ('f3410000-0000-0000-0000-000000000006', 'f3400000-0000-0000-0000-000000000002', 'Mild',           '0',  NULL,      1, true),
    ('f3410000-0000-0000-0000-000000000007', 'f3400000-0000-0000-0000-000000000002', 'Medium',         '0',  NULL,      2, true),
    ('f3410000-0000-0000-0000-000000000008', 'f3400000-0000-0000-0000-000000000002', 'Hot',            '0',  NULL,      3, true),
    ('f3410000-0000-0000-0000-000000000009', 'f3400000-0000-0000-0000-000000000003', 'Thin Crust',     '0',  'VEG',     1, true),
    ('f3410000-0000-0000-0000-000000000010', 'f3400000-0000-0000-0000-000000000003', 'Cheese Burst',   '60', 'VEG',     2, true),
    ('f3410000-0000-0000-0000-000000000011', 'f3400000-0000-0000-0000-000000000004', 'Regular Milk',   '0',  'VEG',     1, true),
    ('f3410000-0000-0000-0000-000000000012', 'f3400000-0000-0000-0000-000000000004', 'Almond Milk',    '40', 'VEG',     2, true),
    ('f3410000-0000-0000-0000-000000000013', 'f3400000-0000-0000-0000-000000000005', 'Less Sugar',     '0',  NULL,      1, true),
    ('f3410000-0000-0000-0000-000000000014', 'f3400000-0000-0000-0000-000000000005', 'No Sugar',       '0',  NULL,      2, true)
) AS v(uuid, group_uuid, name, price, food_type, sort_order, active)
JOIN pos.addon_groups g ON g.uuid = v.group_uuid;

-- ---------------------------------------------------------------- items
-- Items with variants: selling_price = the default variant's price, has_variants = true.
INSERT INTO pos.items (uuid, tenant_uuid, name, short_name, item_code, barcode, category_id, item_type, food_type,
                       unit_of_measure, selling_price, mrp, price_includes_tax, tax_group_id, hsn_sac_code,
                       has_variants, is_open_price, is_favourite, description, sort_order, is_active, is_deleted)
SELECT v.uuid, 'f3d347fa-0029-4d2f-8dec-66e21cec046c', v.name, v.short_name, v.item_code, v.barcode, c.id, v.item_type, v.food_type,
       v.uom, v.price::numeric, v.mrp::numeric, true, tg.id, v.hsn,
       v.has_variants, v.open_price, v.favourite, v.description, v.sort_order, v.active, v.deleted
FROM (VALUES
    -- Breakfast
    ('f3500000-0000-0000-0000-000000000001', 'Idli (2 pcs)',            'Idli',       '101', NULL,            'f3300000-0000-0000-0000-000000000011', 'GOODS', 'VEG',     'PLATE', '40',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, true,  'With sambar and chutney',            1, true,  false),
    ('f3500000-0000-0000-0000-000000000002', 'Masala Dosa',             'M Dosa',     '102', NULL,            'f3300000-0000-0000-0000-000000000011', 'GOODS', 'VEG',     'PLATE', '70',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, true,  NULL,                                 2, true,  false),
    ('f3500000-0000-0000-0000-000000000003', 'Poha',                    'Poha',       '103', NULL,            'f3300000-0000-0000-0000-000000000011', 'GOODS', 'VEG',     'PLATE', '45',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 3, true,  false),
    ('f3500000-0000-0000-0000-000000000004', 'Egg Omelette',            'Omelette',   '104', NULL,            'f3300000-0000-0000-0000-000000000011', 'GOODS', 'EGG',     'PLATE', '50',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 4, true,  false),
    -- Burgers
    ('f3500000-0000-0000-0000-000000000005', 'Veg Burger',              'Veg Burger', '201', NULL,            'f3300000-0000-0000-0000-000000000012', 'GOODS', 'VEG',     'PCS',   '90',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, true,  NULL,                                 1, true,  false),
    ('f3500000-0000-0000-0000-000000000006', 'Chicken Burger',          'Chk Burger', '202', NULL,            'f3300000-0000-0000-0000-000000000012', 'GOODS', 'NON_VEG', 'PCS',   '130', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 2, true,  false),
    ('f3500000-0000-0000-0000-000000000007', 'Paneer Burger',           'Pnr Burger', '203', NULL,            'f3300000-0000-0000-0000-000000000012', 'GOODS', 'VEG',     'PCS',   '120', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, 'Paneer out today (inactive)',        3, false, false),
    -- Pizza (sizes as variants)
    ('f3500000-0000-0000-0000-000000000008', 'Margherita Pizza',        'Margherita', '301', NULL,            'f3300000-0000-0000-0000-000000000013', 'GOODS', 'VEG',     'PCS',   '199', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   true,  false, true,  NULL,                                 1, true,  false),
    ('f3500000-0000-0000-0000-000000000009', 'Chicken Tikka Pizza',     'Chk Pizza',  '302', NULL,            'f3300000-0000-0000-0000-000000000013', 'GOODS', 'NON_VEG', 'PCS',   '299', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   true,  false, false, NULL,                                 2, true,  false),
    -- Snacks
    ('f3500000-0000-0000-0000-000000000010', 'French Fries',            'Fries',      '401', NULL,            'f3300000-0000-0000-0000-000000000014', 'GOODS', 'VEG',     'PLATE', '80',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   true,  false, true,  NULL,                                 1, true,  false),
    ('f3500000-0000-0000-0000-000000000011', 'Veg Spring Roll',         'Spr Roll',   '402', NULL,            'f3300000-0000-0000-0000-000000000014', 'GOODS', 'VEG',     'PLATE', '100', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 2, true,  false),
    ('f3500000-0000-0000-0000-000000000012', 'Chicken Nuggets (6 pcs)', 'Nuggets',    '403', NULL,            'f3300000-0000-0000-0000-000000000014', 'GOODS', 'NON_VEG', 'PLATE', '140', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 3, true,  false),
    -- Hot drinks
    ('f3500000-0000-0000-0000-000000000013', 'Masala Chai',             'Chai',       '501', NULL,            'f3300000-0000-0000-0000-000000000021', 'GOODS', 'VEG',     'CUP',   '20',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   true,  false, true,  NULL,                                 1, true,  false),
    ('f3500000-0000-0000-0000-000000000014', 'Cappuccino',              'Cappuccino', '502', NULL,            'f3300000-0000-0000-0000-000000000021', 'GOODS', 'VEG',     'CUP',   '120', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 2, true,  false),
    ('f3500000-0000-0000-0000-000000000015', 'Filter Coffee',           'Coffee',     '503', NULL,            'f3300000-0000-0000-0000-000000000021', 'GOODS', 'VEG',     'CUP',   '35',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 3, true,  false),
    -- Cold drinks
    ('f3500000-0000-0000-0000-000000000016', 'Fresh Lime Soda',         'Lime Soda',  '601', NULL,            'f3300000-0000-0000-0000-000000000022', 'GOODS', 'VEG',     'GLASS', '60',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   true,  false, false, NULL,                                 1, true,  false),
    ('f3500000-0000-0000-0000-000000000017', 'Iced Tea',                'Iced Tea',   '602', NULL,            'f3300000-0000-0000-0000-000000000022', 'GOODS', 'VEG',     'GLASS', '90',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 2, true,  false),
    -- Shakes
    ('f3500000-0000-0000-0000-000000000018', 'Chocolate Shake',         'Choc Shake', '701', NULL,            'f3300000-0000-0000-0000-000000000023', 'GOODS', 'VEG',     'GLASS', '130', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, true,  NULL,                                 1, true,  false),
    ('f3500000-0000-0000-0000-000000000019', 'Mango Shake',             'Mango Shk',  '702', NULL,            'f3300000-0000-0000-0000-000000000023', 'GOODS', 'VEG',     'GLASS', '120', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 2, true,  false),
    -- Desserts
    ('f3500000-0000-0000-0000-000000000020', 'Gulab Jamun (2 pcs)',     'Jamun',      '801', NULL,            'f3300000-0000-0000-0000-000000000003', 'GOODS', 'VEG',     'PLATE', '50',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 1, true,  false),
    ('f3500000-0000-0000-0000-000000000021', 'Brownie with Ice Cream',  'Brownie',    '802', NULL,            'f3300000-0000-0000-0000-000000000003', 'GOODS', 'EGG',     'PLATE', '150', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                 2, true,  false),
    -- Packaged (sold at MRP, scanned by barcode)
    ('f3500000-0000-0000-0000-000000000022', 'Cola 500ml',              'Cola 500',   '901', '8900000000011', 'f3300000-0000-0000-0000-000000000004', 'GOODS', 'VEG',     'BTL',   '40',  '40', 'f3200000-0000-0000-0000-000000000004', '22021010', false, false, false, 'Packaged, sold at MRP',              1, true,  false),
    ('f3500000-0000-0000-0000-000000000023', 'Mineral Water 1L',        'Water 1L',   '902', '8900000000028', 'f3300000-0000-0000-0000-000000000004', 'GOODS', 'VEG',     'BTL',   '20',  '20', 'f3200000-0000-0000-0000-000000000004', '22011010', false, false, true,  NULL,                                 2, true,  false),
    ('f3500000-0000-0000-0000-000000000024', 'Potato Chips',            'Chips',      '903', NULL,            'f3300000-0000-0000-0000-000000000004', 'GOODS', 'VEG',     'PCS',   '20',  NULL, 'f3200000-0000-0000-0000-000000000003', '20052000', true,  false, false, 'Pack sizes have their own barcodes', 3, true,  false),
    -- Combos (inactive category)
    ('f3500000-0000-0000-0000-000000000025', 'Burger + Fries + Cola',   'Combo 1',    '951', NULL,            'f3300000-0000-0000-0000-000000000005', 'GOODS', 'VEG',     'PCS',   '179', NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, 'In an inactive category',            1, true,  false),
    -- Special cases
    ('f3500000-0000-0000-0000-000000000026', 'Open Item',               'Open',       '999', NULL,            NULL,                                   'GOODS', NULL,      'PCS',   '0',   NULL, 'f3200000-0000-0000-0000-000000000001', NULL,       false, true,  false, 'Open price: cashier types amount',   99, true, false),
    ('f3500000-0000-0000-0000-000000000027', 'Parcel Charge',           'Parcel',     '998', NULL,            NULL,                                   'SERVICE', NULL,    'PCS',   '10',  NULL, 'f3200000-0000-0000-0000-000000000004', '998599',   false, false, false, 'Takeaway packing',                   98, true, false),
    ('f3500000-0000-0000-0000-000000000028', 'Old Sandwich',            'Old Sndw',   '299', NULL,            'f3300000-0000-0000-0000-000000000014', 'GOODS', 'VEG',     'PCS',   '60',  NULL, 'f3200000-0000-0000-0000-000000000002', '996331',   false, false, false, 'Soft-deleted: must never show up',   9, true,  true)
) AS v(uuid, name, short_name, item_code, barcode, category_uuid, item_type, food_type, uom, price, mrp,
       tax_group_uuid, hsn, has_variants, open_price, favourite, description, sort_order, active, deleted)
LEFT JOIN pos.categories c ON c.uuid = v.category_uuid
LEFT JOIN pos.tax_groups tg ON tg.uuid = v.tax_group_uuid;

-- ---------------------------------------------------------------- variants (exactly one active default per item)
INSERT INTO pos.item_variants (uuid, tenant_uuid, item_id, name, item_code, barcode, selling_price, mrp, is_default, sort_order, is_active)
SELECT v.uuid, i.tenant_uuid, i.id, v.name, v.item_code, v.barcode, v.price::numeric, v.mrp::numeric, v.is_default, v.sort_order, true
FROM (VALUES
    ('f3510000-0000-0000-0000-000000000001', 'f3500000-0000-0000-0000-000000000008', 'Regular (7")', '301R', NULL,            '199', NULL, true,  1),
    ('f3510000-0000-0000-0000-000000000002', 'f3500000-0000-0000-0000-000000000008', 'Medium (10")', '301M', NULL,            '349', NULL, false, 2),
    ('f3510000-0000-0000-0000-000000000003', 'f3500000-0000-0000-0000-000000000008', 'Large (12")',  '301L', NULL,            '499', NULL, false, 3),
    ('f3510000-0000-0000-0000-000000000004', 'f3500000-0000-0000-0000-000000000009', 'Regular (7")', '302R', NULL,            '299', NULL, true,  1),
    ('f3510000-0000-0000-0000-000000000005', 'f3500000-0000-0000-0000-000000000009', 'Medium (10")', '302M', NULL,            '449', NULL, false, 2),
    ('f3510000-0000-0000-0000-000000000006', 'f3500000-0000-0000-0000-000000000010', 'Regular',      '401R', NULL,            '80',  NULL, true,  1),
    ('f3510000-0000-0000-0000-000000000007', 'f3500000-0000-0000-0000-000000000010', 'Large',        '401L', NULL,            '120', NULL, false, 2),
    ('f3510000-0000-0000-0000-000000000008', 'f3500000-0000-0000-0000-000000000013', 'Small',        '501S', NULL,            '20',  NULL, true,  1),
    ('f3510000-0000-0000-0000-000000000009', 'f3500000-0000-0000-0000-000000000013', 'Large',        '501L', NULL,            '30',  NULL, false, 2),
    ('f3510000-0000-0000-0000-000000000010', 'f3500000-0000-0000-0000-000000000016', 'Sweet',        '601S', NULL,            '60',  NULL, true,  1),
    ('f3510000-0000-0000-0000-000000000011', 'f3500000-0000-0000-0000-000000000016', 'Salted',       '601T', NULL,            '60',  NULL, false, 2),
    ('f3510000-0000-0000-0000-000000000012', 'f3500000-0000-0000-0000-000000000024', '50g Pack',     NULL,   '8900000000035', '20',  '20', true,  1),
    ('f3510000-0000-0000-0000-000000000013', 'f3500000-0000-0000-0000-000000000024', '100g Pack',    NULL,   '8900000000042', '40',  '40', false, 2)
) AS v(uuid, item_uuid, name, item_code, barcode, price, mrp, is_default, sort_order)
JOIN pos.items i ON i.uuid = v.item_uuid;

-- ---------------------------------------------------------------- item <-> add-on group links
INSERT INTO pos.item_addon_groups (uuid, tenant_uuid, item_id, addon_group_id, sort_order)
SELECT v.uuid, i.tenant_uuid, i.id, g.id, v.sort_order
FROM (VALUES
    ('f3520000-0000-0000-0000-000000000001', 'f3500000-0000-0000-0000-000000000005', 'f3400000-0000-0000-0000-000000000001', 0),  -- Veg Burger: Toppings
    ('f3520000-0000-0000-0000-000000000002', 'f3500000-0000-0000-0000-000000000005', 'f3400000-0000-0000-0000-000000000002', 1),  -- Veg Burger: Spice Level
    ('f3520000-0000-0000-0000-000000000003', 'f3500000-0000-0000-0000-000000000006', 'f3400000-0000-0000-0000-000000000001', 0),  -- Chicken Burger: Toppings
    ('f3520000-0000-0000-0000-000000000004', 'f3500000-0000-0000-0000-000000000006', 'f3400000-0000-0000-0000-000000000002', 1),  -- Chicken Burger: Spice Level
    ('f3520000-0000-0000-0000-000000000005', 'f3500000-0000-0000-0000-000000000008', 'f3400000-0000-0000-0000-000000000003', 0),  -- Margherita: Crust
    ('f3520000-0000-0000-0000-000000000006', 'f3500000-0000-0000-0000-000000000008', 'f3400000-0000-0000-0000-000000000001', 1),  -- Margherita: Toppings
    ('f3520000-0000-0000-0000-000000000007', 'f3500000-0000-0000-0000-000000000009', 'f3400000-0000-0000-0000-000000000003', 0),  -- Chicken Tikka Pizza: Crust
    ('f3520000-0000-0000-0000-000000000008', 'f3500000-0000-0000-0000-000000000009', 'f3400000-0000-0000-0000-000000000001', 1),  -- Chicken Tikka Pizza: Toppings
    ('f3520000-0000-0000-0000-000000000009', 'f3500000-0000-0000-0000-000000000004', 'f3400000-0000-0000-0000-000000000002', 0),  -- Egg Omelette: Spice Level
    ('f3520000-0000-0000-0000-000000000010', 'f3500000-0000-0000-0000-000000000013', 'f3400000-0000-0000-0000-000000000005', 0),  -- Masala Chai: Sugar
    ('f3520000-0000-0000-0000-000000000011', 'f3500000-0000-0000-0000-000000000014', 'f3400000-0000-0000-0000-000000000004', 0),  -- Cappuccino: Milk Options
    ('f3520000-0000-0000-0000-000000000012', 'f3500000-0000-0000-0000-000000000014', 'f3400000-0000-0000-0000-000000000005', 1),  -- Cappuccino: Sugar
    ('f3520000-0000-0000-0000-000000000013', 'f3500000-0000-0000-0000-000000000015', 'f3400000-0000-0000-0000-000000000004', 0),  -- Filter Coffee: Milk Options
    ('f3520000-0000-0000-0000-000000000014', 'f3500000-0000-0000-0000-000000000018', 'f3400000-0000-0000-0000-000000000004', 0)   -- Chocolate Shake: Milk Options
) AS v(uuid, item_uuid, group_uuid, sort_order)
JOIN pos.items i ON i.uuid = v.item_uuid
JOIN pos.addon_groups g ON g.uuid = v.group_uuid;

COMMIT;

-- ---------------------------------------------------------------- summary
SELECT t.what, t.count FROM (
    SELECT '1 tax groups' AS what, count(*) FROM pos.tax_groups WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND is_deleted = false
    UNION ALL SELECT '2 categories', count(*) FROM pos.categories WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND is_deleted = false
    UNION ALL SELECT '3 add-on groups', count(*) FROM pos.addon_groups WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND is_deleted = false
    UNION ALL SELECT '4 add-ons', count(*) FROM pos.addons WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND is_deleted = false
    UNION ALL SELECT '5 items (live)', count(*) FROM pos.items WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND is_deleted = false
    UNION ALL SELECT '6 variants', count(*) FROM pos.item_variants WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND is_deleted = false
    UNION ALL SELECT '7 add-on links', count(*) FROM pos.item_addon_groups WHERE tenant_uuid = 'f3d347fa-0029-4d2f-8dec-66e21cec046c' AND is_deleted = false
) t
ORDER BY t.what;
