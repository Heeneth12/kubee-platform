-- =============================================================
-- KUBEE AUTH SEED DATA
-- Applications, subscription plans, modules and privileges.
-- =============================================================


-- =============================================================
-- 1. APPLICATIONS
-- =============================================================
INSERT INTO auth.applications (id, app_name, app_key, description)
VALUES (1, 'Inventory Management', 'EZH_INV_APP', 'Core system for stock, sales, and purchase tracking'),
       (2, 'Kubee OS', 'KUBEE_POS', 'Point of sale system for orders, billing, and catalog management');


-- =============================================================
-- 2. SUBSCRIPTION PLANS
-- =============================================================
INSERT INTO auth.subscription_plans (application_id, name, description, type, price, duration_days, max_users)
VALUES (1, 'Free Trial', 'Full platform access for 14 days.', 'LIFETIME', 0.00, 14, 2),
       (1, 'Basic', 'Essential tools for small teams.', 'MONTHLY', 9.99, 30, 5),
       (1, 'Pro Monthly', 'Advanced features for growing businesses.', 'MONTHLY', 29.99, 30, 20),
       (1, 'Pro Yearly', 'Best value for established teams. Save 17%.', 'YEARLY', 299.00, 365, 20),
       (1, 'Enterprise', 'Unlimited access with priority support.', 'YEARLY', 999.00, 365, NULL),

       -- KUBEE POS (plan names are globally unique, hence the POS prefix)
       (2, 'POS Free Trial', 'Try Kubee POS free for 14 days.', 'LIFETIME', 0.00, 14, 2),
       (2, 'POS Starter', 'Single counter billing for small shops.', 'MONTHLY', 14.99, 30, 3),
       (2, 'POS Pro Monthly', 'Multiple counters with full reporting.', 'MONTHLY', 39.99, 30, 15),
       (2, 'POS Pro Yearly', 'Pro features billed yearly. Save 17%.', 'YEARLY', 399.00, 365, 15);


-- =============================================================
-- 3. MODULES
-- =============================================================
INSERT INTO auth.modules (id, module_name, module_key, application_id)
VALUES (1, 'Dashboard', 'EZH_INV_DASHBOARD', 1),
       (2, 'Items Management', 'EZH_INV_ITEMS', 1),
       (3, 'Stock Control', 'EZH_INV_STOCK', 1),
       (4, 'Purchases', 'EZH_INV_PURCHASES', 1),
       (5, 'Sales', 'EZH_INV_SALES', 1),
       (6, 'Contacts', 'EZH_INV_CONTACTS', 1),
       (7, 'Employees', 'EZH_INV_EMPLOYEE', 1),
       (8, 'Reports', 'EZH_INV_REPORTS', 1),
       (9, 'Documents', 'EZH_INV_DOCUMENTS', 1),
       (10, 'Admin', 'EZH_INV_USER_MGMT', 1),
       (11, 'Settings', 'EZH_INV_SETTINGS', 1),
       (12, 'Vendor Management', 'EZH_INV_VENDOR', 1),

       -- KUBEE POS
       (13, 'Dashboard', 'KUBEE_POS_DASHBOARD', 2),
       (14, 'Orders', 'KUBEE_POS_ORDERS', 2),
       (15, 'Bills', 'KUBEE_POS_BILLS', 2),
       (16, 'Catalog', 'KUBEE_POS_CATALOG', 2),
       (17, 'Reports', 'KUBEE_POS_REPORTS', 2),
       (18, 'Admin', 'KUBEE_POS_USER_MGMT', 2),
       (19, 'Settings', 'KUBEE_POS_SETTINGS', 2);


-- =============================================================
-- 4. PRIVILEGES
-- =============================================================
INSERT INTO auth.privileges (privilege_name, privilege_key, module_id)
VALUES
    -- DASHBOARD
    ('View Dashboard', 'EZH_INV_DASHBOARD_VIEW', 1),

    -- ITEMS
    ('View Items', 'EZH_INV_ITEMS_VIEW', 2),
    ('Create Items', 'EZH_INV_ITEMS_CREATE', 2),
    ('Edit Items', 'EZH_INV_ITEMS_EDIT', 2),
    ('Delete Items', 'EZH_INV_ITEMS_DELETE', 2),
    ('Export Items', 'EZH_INV_ITEMS_EXPORT', 2),

    -- STOCK
    ('View Stock', 'EZH_INV_STOCK_VIEW', 3),
    ('Adjust Stock', 'EZH_INV_STOCK_EDIT', 3),
    ('Export Stock', 'EZH_INV_STOCK_EXPORT', 3),

    -- PURCHASES
    ('View Purchases', 'EZH_INV_PURCHASES_VIEW', 4),
    ('Create Purchases', 'EZH_INV_PURCHASES_CREATE', 4),
    ('Edit Purchases', 'EZH_INV_PURCHASES_EDIT', 4),
    ('Delete Purchases', 'EZH_INV_PURCHASES_DELETE', 4),
    ('Approve Purchases', 'EZH_INV_PURCHASES_APPROVE', 4),

    -- SALES
    ('View Sales', 'EZH_INV_SALES_VIEW', 5),
    ('Create Sales', 'EZH_INV_SALES_CREATE', 5),
    ('Edit Sales', 'EZH_INV_SALES_EDIT', 5),
    ('Delete Sales', 'EZH_INV_SALES_DELETE', 5),
    ('Approve Sales', 'EZH_INV_SALES_APPROVE', 5),

    -- CONTACTS
    ('View Contacts', 'EZH_INV_CONTACTS_VIEW', 6),
    ('Create Contacts', 'EZH_INV_CONTACTS_CREATE', 6),
    ('Edit Contacts', 'EZH_INV_CONTACTS_EDIT', 6),
    ('Delete Contacts', 'EZH_INV_CONTACTS_DELETE', 6),

    -- EMPLOYEES
    ('View Employees', 'EZH_INV_EMPLOYEE_VIEW', 7),
    ('Create Employees', 'EZH_INV_EMPLOYEE_CREATE', 7),
    ('Edit Employees', 'EZH_INV_EMPLOYEE_EDIT', 7),
    ('Approve Requests', 'EZH_INV_EMPLOYEE_APPROVE', 7),

    -- REPORTS
    ('View Reports', 'EZH_INV_REPORTS_VIEW', 8),
    ('Export Reports', 'EZH_INV_REPORTS_EXPORT', 8),

    -- DOCUMENTS
    ('View Documents', 'EZH_INV_DOCUMENTS_VIEW', 9),
    ('Upload Documents', 'EZH_INV_DOCUMENTS_CREATE', 9),
    ('Delete Documents', 'EZH_INV_DOCUMENTS_DELETE', 9),

    -- ADMIN / USER MGMT
    ('View Users', 'EZH_INV_USER_MGMT_VIEW', 10),
    ('Manage Roles', 'EZH_INV_USER_MGMT_EDIT', 10),

    -- SETTINGS
    ('View Settings', 'EZH_INV_SETTINGS_VIEW', 11),
    ('Update Settings', 'EZH_INV_SETTINGS_EDIT', 11),

    -- VENDOR
    ('View Vendors', 'EZH_INV_VENDOR_VIEW', 12),
    ('Create Vendors', 'EZH_INV_VENDOR_CREATE', 12),
    ('Edit Vendors', 'EZH_INV_VENDOR_EDIT', 12),
    ('Delete Vendors', 'EZH_INV_VENDOR_DELETE', 12),

    -- ===== KUBEE POS =====

    -- DASHBOARD
    ('View Dashboard', 'KUBEE_POS_DASHBOARD_VIEW', 13),

    -- ORDERS
    ('View Orders', 'KUBEE_POS_ORDERS_VIEW', 14),
    ('Create Orders', 'KUBEE_POS_ORDERS_CREATE', 14),
    ('Edit Orders', 'KUBEE_POS_ORDERS_EDIT', 14),
    ('Cancel Orders', 'KUBEE_POS_ORDERS_CANCEL', 14),

    -- BILLS
    ('View Bills', 'KUBEE_POS_BILLS_VIEW', 15),
    ('Create Bills', 'KUBEE_POS_BILLS_CREATE', 15),
    ('Apply Discounts', 'KUBEE_POS_BILLS_DISCOUNT', 15),
    ('Process Refunds', 'KUBEE_POS_BILLS_REFUND', 15),
    ('Void Bills', 'KUBEE_POS_BILLS_VOID', 15),
    ('Print Bills', 'KUBEE_POS_BILLS_PRINT', 15),

    -- CATALOG
    ('View Catalog', 'KUBEE_POS_CATALOG_VIEW', 16),
    ('Create Catalog Items', 'KUBEE_POS_CATALOG_CREATE', 16),
    ('Edit Catalog Items', 'KUBEE_POS_CATALOG_EDIT', 16),
    ('Delete Catalog Items', 'KUBEE_POS_CATALOG_DELETE', 16),
    ('Manage Pricing', 'KUBEE_POS_CATALOG_PRICING', 16),

    -- REPORTS
    ('View Reports', 'KUBEE_POS_REPORTS_VIEW', 17),
    ('Export Reports', 'KUBEE_POS_REPORTS_EXPORT', 17),

    -- ADMIN / USER MGMT
    ('View Users', 'KUBEE_POS_USER_MGMT_VIEW', 18),
    ('Manage Roles', 'KUBEE_POS_USER_MGMT_EDIT', 18),

    -- SETTINGS
    ('View Settings', 'KUBEE_POS_SETTINGS_VIEW', 19),
    ('Update Settings', 'KUBEE_POS_SETTINGS_EDIT', 19);


-- =============================================================
-- 5. SEQUENCE SYNC
-- Inserts with explicit ids don't advance BIGSERIAL sequences;
-- move them past the seeded rows so the next insert doesn't collide.
-- =============================================================
SELECT setval('auth.applications_id_seq', (SELECT MAX(id) FROM auth.applications));
SELECT setval('auth.modules_id_seq', (SELECT MAX(id) FROM auth.modules));
