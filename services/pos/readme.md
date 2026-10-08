# Kubee POS

A simple, fast billing / Point-of-Sale system for small and mid-sized shops in India.

> **Original brief (kept for context):**
> Build a POS with very basic components. The main goal is **not** advanced features — it's getting
> **at least 30 real customers** using the product. Start with the core: catalog, orders, payment, bills.
> Use Petpooja POS as the reference for features, then add our own on top. It must support **instant
> billing** for mid-scale shops: add products → take payment → print/send bill.

---

## 1. Goal & success criteria

| What | Target |
|---|---|
| North-star | **30 shops actively billing** on Kubee POS (≥ 20 bills/day each, for 2+ weeks) |
| Time to onboard a shop | < 15 minutes (signup → first printed bill) |
| Time to make a bill | < 10 seconds for a 5-item bill (keyboard/scanner, no mouse) |
| Works without internet | Yes — billing never stops when the network drops |

Every feature decision is judged by one question: **does this help us get or keep one of the first 30 shops?**
If not, it goes to the backlog.

## 2. Who it's for

**Primary:** counter-billing shops that need speed — supermarkets/kirana, bakeries, sweet shops,
stationery, small pharmacies, quick-service food counters / cafés.

**Their day:** customer walks up → cashier scans or types items → total → customer pays by UPI/cash →
bill printed or sent on WhatsApp → next customer. Peak hours have queues, so speed beats features.

**Secondary (later):** dine-in restaurants (tables, KOT, captains) — Petpooja's core market. We support
this only after the counter flow is solid.

## 3. Reference: Petpooja feature map

Petpooja features studied from **public material only** (website, demo videos, app-store listings, user
reviews) — we re-build the ideas, we don't copy code, UI assets or data.

| Area | Petpooja has | Kubee phase |
|---|---|---|
| Billing | Fast billing, hold/recall bills, item search, barcode, modifiers/add-ons | **MVP** |
| Catalog | Categories, items, variants, prices, tax per item | **MVP** |
| Taxes | GST (CGST/SGST/IGST), HSN, inclusive/exclusive pricing | **MVP** |
| Payments | Cash, card, UPI, split payment, due/credit | **MVP** (cash, UPI, split) / v1 (credit) |
| Bills | Thermal print, reprint, cancel/void, e-bill via SMS/WhatsApp | **MVP** |
| Discounts | Item/bill discounts, coupons | **MVP** (item + bill) / v1 (coupons) |
| Day-end | Cash drawer open/close, shift summary | v1 |
| Reports | 80+ reports: sales, item-wise, tax, payment-mode | MVP (5 core reports) → v1 |
| Customers / CRM | Customer DB, loyalty, feedback | v1 (customer + phone) / v2 (loyalty) |
| Inventory | Stock, purchase, recipe/raw-material management, low-stock alerts | v1 (simple stock) / v2 (recipes) |
| Staff | Roles, permissions, biller-wise reports | v1 |
| Multi-outlet | Central menu, outlet-wise reports | v2 |
| Restaurant | Tables, KOT, KDS, captain app, order types (dine-in/takeaway/delivery) | v2 |
| Online orders | Zomato/Swiggy aggregator integration | v3 (needs partner approvals) |
| Accounting | Tally export, expenses, e-invoicing | v2 |
| Offline mode | Bills while offline, sync later | **MVP** |

## 4. Our differentiators (on top of Petpooja)

1. **Instant billing mode** — keyboard-first screen: type/scan → Enter → next item; `F2` pay cash,
   `F3` pay UPI, `F4` print. No popups in the happy path.
2. **Dynamic UPI QR with auto-confirm** — QR for the exact amount shows on screen / customer display;
   the bill closes itself when payment is confirmed (no "did it come?" checking phones).
3. **WhatsApp bill by default** — enter phone → bill link sent; saves paper, builds a customer list for the shop.
4. **Zero-setup onboarding** — import catalog from Excel/CSV or a photo of the menu/price list; works on any
   laptop or Android tablet browser, no install.
5. **Cheap & simple pricing** — free during pilot; flat low monthly plan after.

## 5. MVP scope (Phase 1)

### 5.1 Catalog
- Categories, items (name, short code, barcode, price, MRP, unit, GST %, HSN, active flag)
- Variants (e.g. size: 250g / 500g / 1kg) with their own price & barcode
- Add-ons/modifiers (for food counters) — optional per item
- Bulk import/export via CSV/Excel
- Quick-keys: pin top 20–30 items as buttons for touch billing

### 5.2 Orders / Billing
- Billing screen: search by name/code/barcode, qty edit, line discount, remove line
- Bill-level discount (₹ or %), round-off
- **Hold / recall** bills (customer went to fetch one more item)
- Optional customer phone/name on bill
- Order states: `DRAFT → HELD → PAID → (CANCELLED | REFUNDED)`
- Every change is audited (who, when, what)

### 5.3 Payments
- Modes: Cash (with change calculation), UPI, Card (recorded — card machine is separate), **Split**
- UPI phase A: static shop QR + manual confirm
- UPI phase B: dynamic QR through a payment gateway (Razorpay / PhonePe PG / Paytm) with webhook auto-confirm
- Refund / void with reason (permission-controlled)

### 5.4 Bills / Invoices
- GST-compliant invoice: shop name, address, GSTIN, invoice no., date, HSN, tax split (CGST/SGST or IGST), totals in words
- Invoice numbering: sequential, per financial year, per device series (e.g. `A1-24-25/000123`) so offline devices never collide
- Print on 58mm / 80mm thermal printers (ESC/POS) and A4
- Reprint, share as PDF/link on WhatsApp/SMS
- Bill template settings: logo, header/footer text, show/hide fields

### 5.5 Basic reports
1. Day sales summary (total, bills count, avg bill value)
2. Payment-mode wise (cash / UPI / card)
3. Item-wise sales
4. GST summary (tax collected by rate) — for the shop's CA
5. Cancelled/refunded bills

### 5.6 Shop setup & users
- Signup with phone OTP, create shop (name, address, GSTIN optional)
- Roles: **Owner**, **Cashier** (cashier cannot delete bills, edit prices, or see reports by default)
- Settings: tax inclusive/exclusive, round-off rule, printer, bill template

## 6. Later phases

| Phase | Theme | Key features |
|---|---|---|
| **v1** — "keep them" | Daily operations | Shift open/close & cash tally, simple stock (in/out, low-stock alert), customer list & credit (khata), coupons, more reports, Excel export, staff permissions |
| **v2** — "grow them" | Restaurants & multi-store | Tables, KOT printing, KDS screen, order types, recipes/raw materials, multi-outlet, Tally export, expenses, loyalty points, e-invoicing (IRN) |
| **v3** — "platform" | Integrations | Zomato/Swiggy orders, online store/QR menu ordering, Android captain app, weighing-scale integration, analytics dashboard |

## 7. Architecture

Reuses the existing Kubee stack (`ezinventory`, `kubee-auth`, `ez-inventory-ui`, `kubee-workspace`).

```
 ┌───────────────────────────── Shop counter ──────────────────────────────┐
 │  Browser / Android tablet (PWA, Angular)                                  │
 │   ├─ Billing UI (keyboard + touch)                                        │
 │   ├─ IndexedDB: catalog cache, offline bills, sync queue                  │
 │   └─ Print: browser print (CSS 58/80mm)  ─or─  local print bridge (ESC/POS)│
 │        ▲ USB/BT barcode scanner (acts as keyboard)                        │
 └────────┼──────────────────────────────────────────────────────────────────┘
          │ HTTPS (REST) + sync API
 ┌────────▼──────────────────────── Cloud ───────────────────────────────────┐
 │  kubee-auth (OTP login, JWT, roles)                                        │
 │  pos-api (Spring Boot)  ── catalog · orders · payments · invoices · reports│
 │     │                     └─ webhook ← Payment gateway (UPI dynamic QR)    │
 │     ├─ PostgreSQL (Flyway migrations, tenant_id on every table)            │
 │     ├─ S3: logos, invoice PDFs, imports                                    │
 │     └─ WhatsApp/SMS provider (bill links)                                  │
 └────────────────────────────────────────────────────────────────────────────┘
```

### Tech stack
| Layer | Choice | Why |
|---|---|---|
| Frontend | Angular 18 + Tailwind, installable **PWA** | Same as `ez-inventory-ui`; PWA gives offline + "install on tablet" |
| Offline store | IndexedDB (via Dexie or idb) | Catalog + unsynced bills survive refresh/power cut |
| Backend | Spring Boot 4, Java 21 | Same as `ezinventory` |
| Auth | `kubee-auth` (reuse) | Already built |
| DB | PostgreSQL + Flyway | Same as existing |
| Files | AWS S3 | Already used in `ezinventory` |
| Printing | Browser print first; small local bridge (ESC/POS over USB/LAN) for silent printing + cash drawer | Start simple, upgrade when shops ask |
| Hosting | Backend: Docker on AWS/Render; Frontend: Vercel | Matches current deploys |

### Offline-first rules
- Catalog, tax settings and bill template are cached on the device; refreshed on login and every few minutes.
- A bill is **created and printed locally first**, then pushed to the server through a sync queue (idempotent by `client_bill_id` UUID).
- Each device gets its own invoice series, so numbers are unique without the server.
- Dynamic UPI needs internet; when offline, fall back to static QR + manual confirm.

### Code layout (DDD + CQRS modular monolith)

```
com.kubee.pos
├── common/            shared kernel: AggregateRoot, BaseEntity, Money, Guard, DomainException,
│                      CommandBus/QueryBus, TenantContext + TenantFilter, ApiResponse, PageResult
└── <module>/          catalog · ordering (orders + payments) · billing (GST invoice) · reporting · shift — built; tax · shop (next)
    ├── domain/        aggregates + business rules, repository interfaces, domain events
    ├── application/   command/ (writes, one handler per command) · query/ (views, read handlers) · port/
    ├── infrastructure/ Spring Data repos (implement domain interfaces), JDBC read model, adapters
    └── api/           *CommandController / *QueryController + request DTOs
```

- Writes: controller → `CommandBus` → handler loads aggregate → domain method → save (events published).
- Reads: controller → `QueryBus` → handler → JDBC query → view DTO (no aggregates loaded).
- Tenant isolation: Hibernate `@TenantId` filters every JPA query; JDBC queries filter `tenant_uuid` explicitly.
  The tenant comes from the kubee-auth JWT `tenantUuid` claim (`JwtAuthFilter` → `TenantFilter` → `TenantContext`).
- Layering is enforced by `ArchitectureTest` (ArchUnit).

**Catalog API** (`/api/v1/catalog`, responses are `{code, message, data}`):
`categories` (GET list / GET one / POST / PUT / DELETE) ·
`items` (GET search `?search&categoryUuid&active&favourite&foodType&page&size`, GET one, GET `lookup?code=`,
POST, PUT full replace incl. variants + add-on groups, PATCH `/{uuid}/active`, PATCH `/{uuid}/favourite`, DELETE) ·
`addon-groups` (GET list / GET one / POST / PUT / DELETE)

**Orders API** (`/api/v1/orders`, every call returns the whole order). Frontend guide: [`docs/orders-api.md`](docs/orders-api.md).
`POST` create (optional lines + discount, idempotent on `clientRef`) · `GET` search
`?status&paymentStatus&orderType&from&to&search&page&size` · `GET /{uuid}` · `PUT /{uuid}/details` ·
`POST /{uuid}/lines` · `PATCH`/`DELETE /{uuid}/lines/{lineUuid}` · `PUT`/`DELETE /{uuid}/discount` ·
`POST /{uuid}/hold | /recall | /complete | /cancel` ·
`POST /{uuid}/payments` (cash with change, UPI, card, split; idempotent on `clientRef`; completes the order when
nothing is due) · `POST /{uuid}/payments/{paymentUuid}/refund`

- Payments live inside the `Order` aggregate (`ordering` module): "never more than due", "complete on the last
  payment" and refund limits are checked in one transaction, and `orders.version` (optimistic lock) stops two
  tills from double-paying. Catalog, settings and order tokens are read through ports
  (`ordering/application/port`) with JDBC adapters, so `ordering` never imports catalog classes.

**Billing API** (`/api/v1/bills`). Frontend guide: [`docs/billing-api.md`](docs/billing-api.md).
`POST` issue for a completed order (idempotent; CGST+SGST in-state, IGST inter-state from the customer's
GSTIN / place of supply) · `GET` search `?status&from&to&search&page&size` · `GET /{uuid}` (lines, tax split,
tax + HSN summary, amount in words) · `GET /api/v1/orders/{orderUuid}/bill` · `POST /{uuid}/print | /share | /cancel`

- Bill numbers `<prefix><series>/<yy-yy>/<000001>` (≤ 16 chars, GST rule), gap-free per series and financial
  year via `number_sequences`. An issued bill is immutable: cancel and re-issue to correct it.
  An order with an issued bill can't be cancelled until the bill is.

**Reports API** (`/api/v1/reports`, all `?from&to`, inclusive, default today). Frontend guide: [`docs/reports-api.md`](docs/reports-api.md).
`sales-summary` (day sales, collections, daily rows) · `payment-modes` · `items` (`?categoryUuid&sort=AMOUNT|QUANTITY`) ·
`gst` (GSTR-1 style: totals, by rate, B2B invoices, B2C by state, HSN; flags completed orders without a bill) ·
`cancellations` (cancelled orders, cancelled bills, refunds) · `hourly` · `staff` · `categories` · `shifts`.
Every report downloads as CSV or Excel: `GET /api/v1/reports/{report}/export?format=csv|xlsx`, and
`GET /api/v1/reports/gstr1?month=yyyy-MM` gives the GSTR-1 JSON for the GST offline tool. Read-only SQL in the
`reporting` module.

**Shifts / cash drawer** (`/api/v1/shifts`). Frontend guide: [`docs/shifts-api.md`](docs/shifts-api.md).
`POST` open (opening cash) · `GET /current` · `POST /{uuid}/cash-movements` (IN/OUT with reason) ·
`POST /{uuid}/close` (counted cash → expected cash and difference). One open shift per shop.

**Permissions.** From the kubee-auth token: role keys `SUPER_ADMIN`, `ADMIN`, `OWNER`, `MANAGER` (configurable:
`pos.security.manager-roles`) or user type `SUPER_ADMIN` / `ADMIN` = **manager**, everyone else = staff.
Manager-only: catalog changes, refunds, cancelling bills, all reports and downloads (checked centrally in
`SecurityConfig`). `GET /api/v1/session` tells the app whether the user is a manager. Errors: `401 Login required`,
`403 Only a manager or the owner can do this`.

**Time zone.** All timestamps are shop-local (`TIMESTAMP` columns, no zone). `PosApplication` sets the JVM zone to
`POS_TIMEZONE` (default `Asia/Kolkata`) before startup, so Java, Hibernate and the PostgreSQL session agree and days
split at the shop's midnight even on a UTC server.

## 8. Data model (MVP)

Full PostgreSQL schema: [`src/main/resources/db/migration/V1__pos_core_schema.sql`](src/main/resources/db/migration/V1__pos_core_schema.sql) (schema `pos`, Flyway-ready).
No inventory yet. Items are just a price list.

```
Flow:  item (Veg Maggi) -> order (cart) -> bill (GST invoice) -> payment
       payments hang off the ORDER, so they can come before or after the bill

Settings  : pos_settings, number_sequences
Taxes     : tax_groups 1--* tax_components            ('GST 5%' = CGST 2.5 + SGST 2.5)
Catalog   : categories 1--* items 1--* item_variants   (Half / Full)
            addon_groups 1--* addons, items *--* addon_groups   (Extra Cheese)
Orders    : orders 1--* order_items 1--* order_item_addons
Payments  : orders 1--* payments                        (split + refunds)
Billing   : orders 1--* bills (one ISSUED at a time) 1--* bill_items 1--* bill_item_taxes
```

Every table has `id BIGSERIAL`, `uuid`, `tenant_uuid` (from the auth service), `created_at`, `updated_at`, `is_deleted`.
Order and bill lines **snapshot** name/price/tax. An issued bill never changes. To fix it, cancel it and issue a new one.

## 9. API outline

```
POST /auth/otp, /auth/verify                     (kubee-auth)
GET/POST/PUT  /catalog/categories, /catalog/items, /catalog/items/import
GET  /catalog/sync?since=<ts>                    → delta for offline cache
POST /bills                                      → create/sync (idempotent on client_bill_id)
POST /bills/{id}/hold | /pay | /cancel | /refund
GET  /bills?date=&status=   GET /bills/{id}/pdf
POST /payments/upi/intent                        → dynamic QR
POST /payments/webhook/{gateway}
POST /bills/{id}/share                           → WhatsApp/SMS link
GET  /reports/day-summary | /payment-modes | /items | /gst | /cancellations
GET/PUT /settings
```

## 10. Roadmap & milestones

| Week | Deliverable | Exit check |
|---|---|---|
| 1 | Project setup, auth integration, tenant/device model, catalog CRUD + CSV import | Can import 500 items in < 1 min |
| 2 | Billing screen (keyboard + touch), GST calc, hold/recall | 5-item bill in < 10 s |
| 3 | Payments (cash, static UPI, split), invoice numbering, thermal print | Print on a real 80mm printer |
| 4 | Offline mode + sync, 5 reports, roles | Bill 30 min with Wi-Fi off, syncs cleanly |
| 5 | WhatsApp bill, onboarding wizard, bug-bash | Fresh shop live in < 15 min |
| 6 | **Pilot with 3–5 shops** | They bill a full day without calling us |
| 7–10 | Fix pilot feedback, dynamic UPI, v1 items shops ask for most | Scale to 30 shops |

## 11. Getting the first 30 customers

1. **Pick one vertical + one area** first (e.g. bakeries/sweet shops in one locality) — word of mouth works inside a niche.
2. **Visit in person**, set it up on their existing laptop/tablet; bring a ₹2–3k thermal printer bundle if needed.
3. **Free for 3 months** for the first 30, in exchange for feedback and a referral.
4. **WhatsApp support group** per shop; reply within minutes during business hours.
5. **Track weekly:** shops onboarded, shops active (billed in last 7 days), bills/day, top complaints.
6. Feature requests from ≥ 3 shops jump the queue; one-off requests wait.

## 12. Non-functional requirements
- Billing screen interaction < 100 ms; works on a 4 GB RAM laptop / mid-range Android tablet
- Data isolation per tenant (every query filtered by `tenant_id`)
- Daily DB backups; bills never hard-deleted
- Indian formats: ₹, lakh/crore separators, DD-MM-YYYY, English first (Hindi/Telugu labels later)

## 13. Open questions
- First vertical to target: retail counters or food counters?
- Payment gateway for dynamic UPI: Razorpay vs PhonePe PG vs Paytm (compare MDR, onboarding time)
- WhatsApp provider: official Cloud API vs a BSP (Gupshup/Interakt) — cost per message
- Pricing after pilot (e.g. ₹299–499/month per counter?)
- Do we need a native Android app in v1, or is the PWA enough?
