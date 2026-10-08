-- =====================================================================
-- Catalog seed data for local testing. NOT a Flyway migration.
--
--   psql -h localhost -U postgres -d kubee -f db/seed/catalog_seed.sql
--
-- Re-runnable: wipes (incl. orders, payments, bills, shifts) and reloads ONLY the two seed tenants below.
-- Run it after the app has started once (so Flyway has created the schema).
--
-- Tenant A  10000000-0000-0000-0000-000000000001  "Maggi Point" (cafe / food counter)
-- Tenant B  10000000-0000-0000-0000-000000000002  "Sri Balaji Kirana" (grocery, barcodes)
--
-- Readable uuid prefixes:  2xxx tax groups/components · 30 categories · 40/41 add-on groups/add-ons
--                          50 items · 51 variants · 52 item <-> add-on group links
-- Tax rates and HSN codes are sample values for testing, not tax advice.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------- reset
-- orders reference items, so test orders/bills/payments/shifts go first (dev data only)
DELETE FROM pos.bill_item_taxes   WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.bill_items        WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.bills             WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.payments          WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002') AND refund_of_payment_id IS NOT NULL;
DELETE FROM pos.payments          WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.order_item_addons WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.order_items       WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.orders            WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.number_sequences  WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.cash_movements    WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.shifts            WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.item_addon_groups WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.item_variants     WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.items             WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.addons            WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.addon_groups      WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.categories        WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002') AND parent_id IS NOT NULL;
DELETE FROM pos.categories        WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.tax_components    WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.tax_groups        WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');
DELETE FROM pos.pos_settings      WHERE tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002');

-- ---------------------------------------------------------------- shop settings (needed to issue bills)
-- GSTINs are made-up test values in a valid format.
INSERT INTO pos.pos_settings (uuid, tenant_uuid, shop_name, address, gstin, state_code, bill_prefix, bill_footer) VALUES
    ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Maggi Point',
     'Near Bus Stand, Hyderabad', '36AAAAA0000A1Z5', '36', 'MP', 'Thank you! Visit again.'),
    ('11000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Sri Balaji Kirana',
     'Market Road, Warangal', '36BBBBB1111B1Z5', '36', 'SB', NULL);

-- ---------------------------------------------------------------- tax groups
INSERT INTO pos.tax_groups (uuid, tenant_uuid, name, total_rate, is_default) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'No Tax',  0,  false),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'GST 5%',  5,  true),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'GST 12%', 12, false),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'GST 18%', 18, false),
    ('20000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000002', 'GST 5%',  5,  true),
    ('20000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000002', 'GST 18%', 18, false);

-- each GST group splits half CGST + half SGST
INSERT INTO pos.tax_components (uuid, tenant_uuid, tax_group_id, name, tax_type, rate, sort_order)
SELECT '21000000-0000-0000-0000-0000000000' || lpad((row_number() OVER (ORDER BY tg.id, c.ord))::text, 2, '0'),
       tg.tenant_uuid, tg.id, c.tax_type, c.tax_type, tg.total_rate / 2, c.ord
FROM pos.tax_groups tg
CROSS JOIN (VALUES ('CGST', 1), ('SGST', 2)) AS c(tax_type, ord)
WHERE tg.tenant_uuid IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002')
  AND tg.total_rate > 0;

-- ---------------------------------------------------------------- categories (top level first)
INSERT INTO pos.categories (uuid, tenant_uuid, name, sort_order, is_active) VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Food',          1, true),
    ('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'Beverages',     2, true),
    ('30000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'Combos',        3, false),
    ('30000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000002', 'Groceries',     1, true),
    ('30000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000002', 'Personal Care', 2, true);

INSERT INTO pos.categories (uuid, tenant_uuid, name, parent_id, sort_order, is_active)
SELECT v.uuid, p.tenant_uuid, v.name, p.id, v.sort_order, true
FROM (VALUES
    ('30000000-0000-0000-0000-000000000002', 'Maggi',       '30000000-0000-0000-0000-000000000001', 1),
    ('30000000-0000-0000-0000-000000000003', 'Sandwiches',  '30000000-0000-0000-0000-000000000001', 2),
    ('30000000-0000-0000-0000-000000000004', 'Snacks',      '30000000-0000-0000-0000-000000000001', 3),
    ('30000000-0000-0000-0000-000000000006', 'Hot Drinks',  '30000000-0000-0000-0000-000000000005', 1),
    ('30000000-0000-0000-0000-000000000007', 'Cold Drinks', '30000000-0000-0000-0000-000000000005', 2)
) AS v(uuid, name, parent_uuid, sort_order)
JOIN pos.categories p ON p.uuid = v.parent_uuid;

-- ---------------------------------------------------------------- add-on groups + add-ons (tenant A)
INSERT INTO pos.addon_groups (uuid, tenant_uuid, name, min_select, max_select, is_active) VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Toppings',     0, 3,    true),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Spice Level',  1, 1,    true),
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'Egg Add-on',   0, 1,    true),
    ('40000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'Milk Options', 0, 1,    true);

INSERT INTO pos.addons (uuid, tenant_uuid, addon_group_id, name, price, food_type, sort_order, is_active)
SELECT v.uuid, g.tenant_uuid, g.id, v.name, v.price::numeric, v.food_type, v.sort_order, v.active
FROM (VALUES
    ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Extra Cheese',       '20', 'VEG', 1, true),
    ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'Extra Butter',       '10', 'VEG', 2, true),
    ('41000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 'Extra Veggies',      '15', 'VEG', 3, true),
    ('41000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000001', 'Peri Peri Sprinkle', '10', 'VEG', 4, false),
    ('41000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000002', 'Mild',               '0',  NULL,  1, true),
    ('41000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000002', 'Medium',             '0',  NULL,  2, true),
    ('41000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000002', 'Spicy',              '0',  NULL,  3, true),
    ('41000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', 'Add Egg',            '15', 'EGG', 1, true),
    ('41000000-0000-0000-0000-000000000009', '40000000-0000-0000-0000-000000000004', 'Regular Milk',       '0',  'VEG', 1, true),
    ('41000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000004', 'Oat Milk',           '30', 'VEG', 2, true)
) AS v(uuid, group_uuid, name, price, food_type, sort_order, active)
JOIN pos.addon_groups g ON g.uuid = v.group_uuid;

-- ---------------------------------------------------------------- items
-- Items with variants: selling_price = the default variant's price, has_variants = true.
INSERT INTO pos.items (uuid, tenant_uuid, name, short_name, item_code, barcode, category_id, item_type, food_type,
                       unit_of_measure, selling_price, mrp, price_includes_tax, tax_group_id, hsn_sac_code,
                       has_variants, is_open_price, is_favourite, description, sort_order, is_active, is_deleted)
SELECT v.uuid, v.tenant_uuid, v.name, v.short_name, v.item_code, v.barcode, c.id, v.item_type, v.food_type,
       v.uom, v.price::numeric, v.mrp::numeric, true, tg.id, v.hsn,
       v.has_variants, v.open_price, v.favourite, v.description, v.sort_order, v.active, v.deleted
FROM (VALUES
    -- tenant A: Maggi Point -------------------------------------------------------------------------------------------------------
    ('50000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Veg Maggi',               'Veg Maggi',  '101', NULL,            '30000000-0000-0000-0000-000000000002', 'GOODS', 'VEG', 'PLATE', '70',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   true,  false, true,  'Classic masala maggi with veggies', 1, true,  false),
    ('50000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Cheese Maggi',            'Chs Maggi',  '102', NULL,            '30000000-0000-0000-0000-000000000002', 'GOODS', 'VEG', 'PLATE', '80',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                2, true,  false),
    ('50000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'Egg Maggi',               'Egg Maggi',  '103', NULL,            '30000000-0000-0000-0000-000000000002', 'GOODS', 'EGG', 'PLATE', '90',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                3, true,  false),
    ('50000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'Masala Maggi',            'Msl Maggi',  '104', NULL,            '30000000-0000-0000-0000-000000000002', 'GOODS', 'VEG', 'PLATE', '60',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, true,  NULL,                                4, true,  false),
    ('50000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'Veg Sandwich',            'Veg Sndw',   '201', NULL,            '30000000-0000-0000-0000-000000000003', 'GOODS', 'VEG', 'PCS',   '60',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                1, true,  false),
    ('50000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'Grilled Cheese Sandwich', 'Grl Chs',    '202', NULL,            '30000000-0000-0000-0000-000000000003', 'GOODS', 'VEG', 'PCS',   '90',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                2, true,  false),
    ('50000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'Paneer Tikka Sandwich',   'Pnr Tikka',  '203', NULL,            '30000000-0000-0000-0000-000000000003', 'GOODS', 'VEG', 'PCS',   '110', NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, 'Out of stock today (inactive)',   3, false, false),
    ('50000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'Samosa',                  'Samosa',     '301', NULL,            '30000000-0000-0000-0000-000000000004', 'GOODS', 'VEG', 'PCS',   '15',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, true,  NULL,                                1, true,  false),
    ('50000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', 'French Fries',            'Fries',      '302', NULL,            '30000000-0000-0000-0000-000000000004', 'GOODS', 'VEG', 'PLATE', '70',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   true,  false, false, NULL,                                2, true,  false),
    ('50000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'Veg Puff',                'Veg Puff',   '303', NULL,            '30000000-0000-0000-0000-000000000004', 'GOODS', 'VEG', 'PCS',   '25',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                3, true,  false),
    ('50000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'Masala Chai',             'Chai',       '401', NULL,            '30000000-0000-0000-0000-000000000006', 'GOODS', 'VEG', 'CUP',   '15',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   true,  false, true,  NULL,                                1, true,  false),
    ('50000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'Filter Coffee',           'Coffee',     '402', NULL,            '30000000-0000-0000-0000-000000000006', 'GOODS', 'VEG', 'CUP',   '30',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                2, true,  false),
    ('50000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', 'Green Tea',               'Green Tea',  '403', NULL,            '30000000-0000-0000-0000-000000000006', 'GOODS', 'VEG', 'CUP',   '25',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                3, true,  false),
    ('50000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', 'Cola 300ml',              'Cola',       '501', '2000000000014', '30000000-0000-0000-0000-000000000007', 'GOODS', 'VEG', 'BTL',   '40',  '40',  '20000000-0000-0000-0000-000000000004', '22021010', false, false, false, 'Packaged, sold at MRP',           1, true,  false),
    ('50000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000001', 'Packaged Water 1L',       'Water 1L',   '502', '2000000000021', '30000000-0000-0000-0000-000000000007', 'GOODS', 'VEG', 'BTL',   '20',  '20',  '20000000-0000-0000-0000-000000000004', '22011010', false, false, false, NULL,                                2, true,  false),
    ('50000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000001', 'Cold Coffee',             'Cold Cof',   '503', NULL,            '30000000-0000-0000-0000-000000000007', 'GOODS', 'VEG', 'GLASS', '80',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, NULL,                                3, true,  false),
    ('50000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000001', 'Fresh Lime Soda',         'Lime Soda',  '504', NULL,            '30000000-0000-0000-0000-000000000007', 'GOODS', 'VEG', 'GLASS', '50',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   true,  false, false, NULL,                                4, true,  false),
    ('50000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000001', 'Maggi + Chai Combo',      'Combo 1',    '601', NULL,            '30000000-0000-0000-0000-000000000008', 'GOODS', 'VEG', 'PCS',   '99',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, 'In an inactive category',         1, false, false),
    ('50000000-0000-0000-0000-000000000019', '10000000-0000-0000-0000-000000000001', 'Misc Item',               'Misc',       '999', NULL,            NULL,                                   'GOODS', NULL,  'PCS',   '0',   NULL,  '20000000-0000-0000-0000-000000000001', NULL,       false, true,  false, 'Open price: cashier types amount', 99, true, false),
    ('50000000-0000-0000-0000-000000000020', '10000000-0000-0000-0000-000000000001', 'Old Burger',              'Burger',     '299', NULL,            '30000000-0000-0000-0000-000000000004', 'GOODS', 'VEG', 'PCS',   '50',  NULL,  '20000000-0000-0000-0000-000000000002', '996331',   false, false, false, 'Soft-deleted: must never show up', 9, true, true),
    -- tenant B: Sri Balaji Kirana (note: item_code 101 / 102 reused on purpose to test tenant isolation) ----------------------------
    ('50000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000002', 'Iodised Salt 1kg',        'Salt 1kg',   '101', '2000000001011', '30000000-0000-0000-0000-000000000021', 'GOODS', NULL,  'PCS',   '28',  '30',  NULL,                                   '25010020', false, false, true,  NULL,                                1, true,  false),
    ('50000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000002', 'Wheat Atta 5kg',          'Atta 5kg',   '102', '2000000001028', '30000000-0000-0000-0000-000000000021', 'GOODS', NULL,  'PCS',   '260', '285', '20000000-0000-0000-0000-000000000021', '11010000', false, false, false, NULL,                                2, true,  false),
    ('50000000-0000-0000-0000-000000000023', '10000000-0000-0000-0000-000000000002', 'Glucose Biscuits',        'Biscuits',   '103', NULL,            '30000000-0000-0000-0000-000000000021', 'GOODS', NULL,  'PCS',   '10',  NULL,  '20000000-0000-0000-0000-000000000021', '19053100', true,  false, false, NULL,                                3, true,  false),
    ('50000000-0000-0000-0000-000000000024', '10000000-0000-0000-0000-000000000002', 'Toothpaste 100g',         'Toothpaste', '201', '2000000002018', '30000000-0000-0000-0000-000000000022', 'GOODS', NULL,  'PCS',   '55',  '60',  '20000000-0000-0000-0000-000000000022', '33061020', false, false, false, NULL,                                1, true,  false)
) AS v(uuid, tenant_uuid, name, short_name, item_code, barcode, category_uuid, item_type, food_type, uom, price, mrp,
       tax_group_uuid, hsn, has_variants, open_price, favourite, description, sort_order, active, deleted)
LEFT JOIN pos.categories c ON c.uuid = v.category_uuid
LEFT JOIN pos.tax_groups tg ON tg.uuid = v.tax_group_uuid;

-- ---------------------------------------------------------------- variants (exactly one active default per item)
INSERT INTO pos.item_variants (uuid, tenant_uuid, item_id, name, item_code, barcode, selling_price, mrp, is_default, sort_order, is_active)
SELECT v.uuid, i.tenant_uuid, i.id, v.name, v.item_code, v.barcode, v.price::numeric, v.mrp::numeric, v.is_default, v.sort_order, true
FROM (VALUES
    ('51000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Half',     '101H', NULL,            '40',  NULL, false, 1),
    ('51000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', 'Full',     '101F', NULL,            '70',  NULL, true,  2),
    ('51000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000009', 'Regular',  '302R', NULL,            '70',  NULL, true,  1),
    ('51000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000009', 'Large',    '302L', NULL,            '100', NULL, false, 2),
    ('51000000-0000-0000-0000-000000000005', '50000000-0000-0000-0000-000000000011', 'Small',    '401S', NULL,            '15',  NULL, true,  1),
    ('51000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000011', 'Large',    '401L', NULL,            '25',  NULL, false, 2),
    ('51000000-0000-0000-0000-000000000007', '50000000-0000-0000-0000-000000000017', 'Sweet',    '504S', NULL,            '50',  NULL, true,  1),
    ('51000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000017', 'Salted',   '504T', NULL,            '50',  NULL, false, 2),
    -- tenant B: pack sizes with their own barcodes
    ('51000000-0000-0000-0000-000000000021', '50000000-0000-0000-0000-000000000023', '100g Pack', NULL,  '2000000001035', '10',  '10', true,  1),
    ('51000000-0000-0000-0000-000000000022', '50000000-0000-0000-0000-000000000023', '250g Pack', NULL,  '2000000001042', '25',  '25', false, 2)
) AS v(uuid, item_uuid, name, item_code, barcode, price, mrp, is_default, sort_order)
JOIN pos.items i ON i.uuid = v.item_uuid;

-- ---------------------------------------------------------------- item <-> add-on group links
INSERT INTO pos.item_addon_groups (uuid, tenant_uuid, item_id, addon_group_id, sort_order)
SELECT v.uuid, i.tenant_uuid, i.id, g.id, v.sort_order
FROM (VALUES
    ('52000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 0),  -- Veg Maggi: Toppings
    ('52000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', 1),  -- Veg Maggi: Spice Level
    ('52000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 0),  -- Cheese Maggi: Toppings
    ('52000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 1),  -- Cheese Maggi: Spice Level
    ('52000000-0000-0000-0000-000000000005', '50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000003', 0),  -- Egg Maggi: Egg Add-on
    ('52000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000002', 1),  -- Egg Maggi: Spice Level
    ('52000000-0000-0000-0000-000000000007', '50000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000001', 0),  -- Grilled Cheese: Toppings
    ('52000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000004', 0),  -- Masala Chai: Milk Options
    ('52000000-0000-0000-0000-000000000009', '50000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000004', 0)   -- Filter Coffee: Milk Options
) AS v(uuid, item_uuid, group_uuid, sort_order)
JOIN pos.items i ON i.uuid = v.item_uuid
JOIN pos.addon_groups g ON g.uuid = v.group_uuid;

COMMIT;

-- ---------------------------------------------------------------- summary
SELECT t.tenant, t.what, t.count FROM (
    SELECT tenant_uuid AS tenant, '1 tax groups' AS what, count(*) FROM pos.tax_groups WHERE is_deleted = false GROUP BY 1
    UNION ALL SELECT tenant_uuid, '2 categories', count(*) FROM pos.categories WHERE is_deleted = false GROUP BY 1
    UNION ALL SELECT tenant_uuid, '3 add-on groups', count(*) FROM pos.addon_groups WHERE is_deleted = false GROUP BY 1
    UNION ALL SELECT tenant_uuid, '4 items (live)', count(*) FROM pos.items WHERE is_deleted = false GROUP BY 1
    UNION ALL SELECT tenant_uuid, '5 variants', count(*) FROM pos.item_variants WHERE is_deleted = false GROUP BY 1
) t
WHERE t.tenant IN ('10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002')
ORDER BY t.tenant, t.what;
