-- =============================================================
-- KUBEE AUTH SCHEMA
-- Creates the auth schema with all tables, constraints and indexes.
-- Seed data lives in V2__seed_data.sql.
-- =============================================================
CREATE SCHEMA IF NOT EXISTS auth;


-- =============================================================
-- 1. APPLICATIONS  (platform-level — not tenant-scoped)
-- =============================================================
CREATE TABLE auth.applications
(
    id          BIGSERIAL PRIMARY KEY,
    app_name    VARCHAR(255) NOT NULL,
    app_key     VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- 2. SUBSCRIPTION PLANS
-- =============================================================
CREATE TABLE auth.subscription_plans
(
    id             BIGSERIAL PRIMARY KEY,
    application_id BIGINT         NOT NULL REFERENCES auth.applications (id) ON DELETE CASCADE,
    name           VARCHAR(100)   NOT NULL UNIQUE,
    description    TEXT,
    type           VARCHAR(20)    NOT NULL CHECK (type IN ('LIFETIME', 'MONTHLY', 'YEARLY')),
    price          NUMERIC(19, 2) NOT NULL DEFAULT 0.00 CHECK (price >= 0),
    duration_days  INTEGER        NOT NULL CHECK (duration_days > 0),
    max_users      INTEGER CHECK (max_users IS NULL OR max_users > 0),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);


-- =============================================================
-- 3. TENANTS
-- =============================================================
CREATE TABLE auth.tenants
(
    id                   BIGSERIAL PRIMARY KEY,
    tenant_uuid          VARCHAR(36)  NOT NULL UNIQUE,
    tenant_name          VARCHAR(255) NOT NULL,
    tenant_code          VARCHAR(100) NOT NULL UNIQUE,
    is_personal          BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_verify            BOOLEAN      NOT NULL DEFAULT FALSE,
    tenant_admin_user_id BIGINT, -- FK added after users table
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- 4. TENANT DETAILS (1:1)
-- =============================================================
CREATE TABLE auth.tenant_details
(
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT       NOT NULL UNIQUE REFERENCES auth.tenants (id) ON DELETE CASCADE,
    business_type      VARCHAR(50)  NOT NULL,
    legal_name         VARCHAR(255) NOT NULL,
    base_currency      CHAR(3)      NOT NULL,
    time_zone          VARCHAR(100) NOT NULL DEFAULT 'UTC',
    gst_number         VARCHAR(50) UNIQUE,
    cin_number         VARCHAR(50) UNIQUE,
    is_gst_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    pan_number         VARCHAR(20),
    trade_name         VARCHAR(255),
    kyc_status         VARCHAR(50),
    incorporation_date TIMESTAMPTZ,
    support_email      VARCHAR(255),
    contact_phone      VARCHAR(50),
    logo_url           TEXT,
    website            TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- 5. SUBSCRIPTIONS
-- =============================================================
CREATE TABLE auth.subscriptions
(
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL REFERENCES auth.tenants (id) ON DELETE CASCADE,
    plan_id    BIGINT      NOT NULL REFERENCES auth.subscription_plans (id),
    status     VARCHAR(20) NOT NULL CHECK (status IN ('TRIAL', 'ACTIVE', 'EXPIRED', 'CANCELLED', 'PENDING_PAYMENT')),
    start_date TIMESTAMPTZ NOT NULL,
    end_date   TIMESTAMPTZ NOT NULL,
    is_primary BOOLEAN     NOT NULL DEFAULT FALSE,
    auto_renew BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_sub_dates CHECK (end_date > start_date)
);

-- Only one ACTIVE or TRIAL subscription per tenant at a time
CREATE UNIQUE INDEX uk_one_active_sub_per_tenant
    ON auth.subscriptions (tenant_id) WHERE status IN ('ACTIVE', 'TRIAL');


-- =============================================================
-- 6. BRANCHES
-- =============================================================
CREATE TABLE auth.branches
(
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL REFERENCES auth.tenants (id) ON DELETE CASCADE,
    branch_name    VARCHAR(255) NOT NULL,
    branch_code    VARCHAR(100) NOT NULL,
    is_head_office BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_branch_code UNIQUE (tenant_id, branch_code)
);

-- Only one head office per tenant
CREATE UNIQUE INDEX uk_one_head_office_per_tenant
    ON auth.branches (tenant_id) WHERE is_head_office = TRUE;


-- =============================================================
-- 7. MODULES  (belongs to application)
-- =============================================================
CREATE TABLE auth.modules
(
    id             BIGSERIAL PRIMARY KEY,
    application_id BIGINT       NOT NULL REFERENCES auth.applications (id) ON DELETE CASCADE,
    module_name    VARCHAR(255) NOT NULL,
    module_key     VARCHAR(100) NOT NULL,
    description    TEXT,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_app_module_key UNIQUE (application_id, module_key)
);


-- =============================================================
-- 8. PRIVILEGES  (belongs to module)
-- =============================================================
CREATE TABLE auth.privileges
(
    id             BIGSERIAL PRIMARY KEY,
    module_id      BIGINT       NOT NULL REFERENCES auth.modules (id) ON DELETE CASCADE,
    privilege_name VARCHAR(255) NOT NULL,
    privilege_key  VARCHAR(100) NOT NULL,
    description    TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_module_privilege_key UNIQUE (module_id, privilege_key)
);


-- =============================================================
-- 9. USERS
-- branch_id NULL → TENANT_ADMIN (no branch scope)
-- branch_id SET  → branch-scoped user
-- =============================================================
CREATE TABLE auth.users
(
    id                   BIGSERIAL PRIMARY KEY,
    user_uuid            VARCHAR(50)  NOT NULL UNIQUE,
    tenant_id            BIGINT       NOT NULL REFERENCES auth.tenants (id) ON DELETE CASCADE,
    branch_id            BIGINT REFERENCES auth.branches (id) ON DELETE RESTRICT,
    full_name            VARCHAR(255) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    phone                VARCHAR(50),
    profile_picture_uuid VARCHAR(100),
    account_scope        VARCHAR(50)  NOT NULL,
    user_type            VARCHAR(50)  NOT NULL,
    is_login_enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Circular FK: tenants → users
ALTER TABLE auth.tenants
    ADD CONSTRAINT fk_tenant_admin_user
        FOREIGN KEY (tenant_admin_user_id) REFERENCES auth.users (id) ON DELETE SET NULL;


-- =============================================================
-- 10. ROLES  (tenant-level templates)
-- =============================================================
CREATE TABLE auth.roles
(
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL REFERENCES auth.tenants (id) ON DELETE CASCADE,
    role_name      VARCHAR(255) NOT NULL,
    role_key       VARCHAR(100) NOT NULL,
    description    TEXT,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    is_system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_tenant_role_name UNIQUE (tenant_id, role_name),
    CONSTRAINT uk_tenant_role_key UNIQUE (tenant_id, role_key)
);


-- =============================================================
-- 11. ROLE PRIVILEGES  (what each role template contains)
-- =============================================================
CREATE TABLE auth.role_privileges
(
    id           BIGSERIAL PRIMARY KEY,
    role_id      BIGINT      NOT NULL REFERENCES auth.roles (id) ON DELETE CASCADE,
    privilege_id BIGINT      NOT NULL REFERENCES auth.privileges (id) ON DELETE CASCADE,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_role_privilege UNIQUE (role_id, privilege_id)
);


-- =============================================================
-- 12. USER ROLES
-- =============================================================
CREATE TABLE auth.user_roles
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
    role_id     BIGINT      NOT NULL REFERENCES auth.roles (id) ON DELETE CASCADE,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by BIGINT,
    expires_at  TIMESTAMPTZ,

    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT chk_role_expiry CHECK (expires_at IS NULL OR expires_at > assigned_at)
);


-- =============================================================
-- 13. USER APPLICATIONS  (which apps a user can access)
-- =============================================================
CREATE TABLE auth.user_applications
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
    application_id BIGINT      NOT NULL REFERENCES auth.applications (id) ON DELETE CASCADE,
    is_active      BOOLEAN     NOT NULL DEFAULT TRUE,
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_user_application UNIQUE (user_id, application_id)
);


-- =============================================================
-- 14. USER MODULE PRIVILEGES  (exact privilege list per user per app)
-- =============================================================
CREATE TABLE auth.user_module_privileges
(
    id                  BIGSERIAL PRIMARY KEY,
    user_application_id BIGINT  NOT NULL REFERENCES auth.user_applications (id) ON DELETE CASCADE,
    privilege_id        BIGINT  NOT NULL REFERENCES auth.privileges (id) ON DELETE CASCADE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_user_app_privilege UNIQUE (user_application_id, privilege_id)
);


-- =============================================================
-- 15. TENANT APPLICATIONS  (which apps a tenant is subscribed to)
-- =============================================================
CREATE TABLE auth.tenant_applications
(
    tenant_id      BIGINT      NOT NULL REFERENCES auth.tenants (id) ON DELETE CASCADE,
    application_id BIGINT      NOT NULL REFERENCES auth.applications (id) ON DELETE CASCADE,
    enabled_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tenant_app PRIMARY KEY (tenant_id, application_id)
);


-- =============================================================
-- 16. ADDRESSES  (unified polymorphic)
-- entity_type : TENANT | USER | BRANCH
-- entity_id   : id of the owning row
-- =============================================================
CREATE TABLE auth.addresses
(
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(10) NOT NULL CHECK (entity_type IN ('TENANT', 'USER', 'BRANCH')),
    entity_id     BIGINT      NOT NULL,
    address_type  VARCHAR(20) NOT NULL CHECK (address_type IN
        ('BILLING', 'SHIPPING', 'REGISTERED', 'OPERATIONAL', 'OFFICE', 'HOME', 'OTHER')),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    route         VARCHAR(100),
    area          VARCHAR(100),
    city          VARCHAR(100),
    state         VARCHAR(100),
    country       VARCHAR(100),
    pin_code      VARCHAR(20),
    is_primary    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One primary address per entity per address_type
CREATE UNIQUE INDEX uk_primary_address
    ON auth.addresses (entity_type, entity_id, address_type) WHERE is_primary = TRUE;


-- =============================================================
-- 17. INTEGRATIONS  (per-tenant third-party connections)
-- =============================================================
CREATE TABLE auth.integrations
(
    id               BIGSERIAL PRIMARY KEY,
    integration_uuid VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id        BIGINT       NOT NULL REFERENCES auth.tenants (id) ON DELETE CASCADE,
    integration_type VARCHAR(100) NOT NULL,
    display_name     VARCHAR(255),
    primary_key      TEXT,
    secondary_key    TEXT,
    tertiary_key     TEXT,
    is_test_mode     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_connected     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    webhook_config   TEXT,
    extra_config     TEXT,
    links            TEXT,
    connected_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_tenant_integration_type UNIQUE (tenant_id, integration_type)
);


-- =============================================================
-- 18. USER REQUESTS  (support tickets)
-- =============================================================
CREATE TABLE auth.user_requests
(
    id            BIGSERIAL PRIMARY KEY,
    user_req_uuid VARCHAR(50)  NOT NULL UNIQUE,
    tenant_uuid   VARCHAR(50),
    user_uuid     VARCHAR(50),
    assigned_uuid VARCHAR(50),
    contact_email VARCHAR(255) NOT NULL,
    contact_name  VARCHAR(255),
    subject       VARCHAR(255) NOT NULL,
    description   TEXT         NOT NULL,
    source_url    TEXT         NOT NULL,
    source_name   VARCHAR(100) NOT NULL,
    category      VARCHAR(50)  NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    priority      VARCHAR(50)  NOT NULL,
    metadata      JSONB DEFAULT '{}'::jsonb,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW(),
    resolved_at   TIMESTAMPTZ
);


-- =============================================================
-- 19. INDEXES
-- =============================================================

-- tenants
CREATE INDEX idx_tenants_is_active ON auth.tenants (is_active);

-- branches
CREATE INDEX idx_branches_tenant_id ON auth.branches (tenant_id);
CREATE INDEX idx_branches_tenant_active ON auth.branches (tenant_id, is_active);

-- users
CREATE INDEX idx_users_tenant_id ON auth.users (tenant_id);
CREATE INDEX idx_users_branch_id ON auth.users (branch_id);
CREATE INDEX idx_users_tenant_type_active ON auth.users (tenant_id, user_type, is_active);

-- roles
CREATE INDEX idx_roles_tenant_active ON auth.roles (tenant_id, is_active);

-- role_privileges
CREATE INDEX idx_rp_privilege_id ON auth.role_privileges (privilege_id);

-- user_roles
CREATE INDEX idx_ur_role_id ON auth.user_roles (role_id);
CREATE INDEX idx_ur_user_active ON auth.user_roles (user_id, is_active);

-- user_applications
CREATE INDEX idx_ua_application_id ON auth.user_applications (application_id);
CREATE INDEX idx_ua_user_active ON auth.user_applications (user_id, is_active);

-- user_module_privileges
CREATE INDEX idx_ump_privilege_id ON auth.user_module_privileges (privilege_id);

-- tenant_applications
CREATE INDEX idx_ta_application_id ON auth.tenant_applications (application_id);

-- subscriptions
CREATE INDEX idx_sub_tenant_status ON auth.subscriptions (tenant_id, status);
CREATE INDEX idx_sub_plan_id ON auth.subscriptions (plan_id);

-- subscription_plans
CREATE INDEX idx_plans_application_id ON auth.subscription_plans (application_id);

-- modules
CREATE INDEX idx_modules_active ON auth.modules (application_id, is_active);

-- addresses
CREATE INDEX idx_addresses_entity ON auth.addresses (entity_type, entity_id);

-- integrations
CREATE INDEX idx_integrations_type ON auth.integrations (integration_type);

-- user_requests
CREATE INDEX idx_user_requests_email ON auth.user_requests (contact_email);
CREATE INDEX idx_user_requests_tenant_category ON auth.user_requests (tenant_uuid, category);
CREATE INDEX idx_user_requests_tenant_status ON auth.user_requests (tenant_uuid, status);
