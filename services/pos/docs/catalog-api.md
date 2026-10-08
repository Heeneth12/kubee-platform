# Catalog API: Frontend Guide (Angular)

Everything the Angular app needs to build the **catalog** feature: endpoints, request/response models as
TypeScript types, and a ready-to-use service. All shapes below are taken from real API responses.

- Backend: Kubee POS (`pos`), default port **8086**
- Base path: **`/api/v1/catalog`**
- Covers: **Categories**, **Items** (with variants), **Add-on groups** (with add-ons)

---

## 1. Basics

### Headers

| Header | Required | Value |
|---|---|---|
| `Authorization` | **yes** | `Bearer <kubee-auth access token>`. The shop (tenant) and user come from its `tenantUuid` / `userUuid` claims. |
| `Content-Type` | for POST/PUT/PATCH | `application/json` |

### Response envelope

Every response, success or error, has the same envelope:

```json
{ "code": 200, "message": "OK", "data": { } }
```

- `data` is **omitted** (not `null`) when there is nothing to return (e.g. delete, most errors).
- Inside `data`, optional fields are present with value **`null`** (not omitted).

### Permissions

Two levels, taken from the kubee-auth token: **manager** (role key `SUPER_ADMIN`, `ADMIN`, `OWNER` or `MANAGER`,
or user type `SUPER_ADMIN` / `ADMIN`; the role list is set by `pos.security.manager-roles`) and **staff**
(everyone else, e.g. cashiers). `GET /api/v1/session` returns `{ userUuid, email, tenantUuid, userType, roles,
manager }`: use `manager` to show or hide screens.

Manager-only (staff get `403`):
- Catalog changes: every `POST` / `PUT` / `PATCH` / `DELETE` under `/api/v1/catalog` (prices, items, categories, add-ons)
- Refunds: `POST /api/v1/orders/{uuid}/payments/{paymentUuid}/refund`
- Cancelling a bill: `POST /api/v1/bills/{uuid}/cancel`
- All reports, exports and GSTR-1: `GET /api/v1/reports/**`

Staff can do everything else: read the catalog, take orders, discounts, hold/recall, take payments, cancel an
unpaid order, issue/print bills, open/close shifts and record cash in/out.

### Errors

| Status | When | `message` / `data` |
|---|---|---|
| `400` | Field validation failed | `message: "Validation failed"`, `data: { field: error }` |
| `400` | Bad JSON / unknown enum value / bad query param | `message: "Malformed request: ..."` |
| `401` | Token missing, invalid or expired (`Login required`), or it has no `tenantUuid` claim | send the user to login |
| `403` | `Only a manager or the owner can do this` (see Permissions below) | hide / disable that action for this user |
| `404` | Record not found **for this tenant** (or soft-deleted) | `message` only |
| `409` | Duplicate (name / item code / barcode) or record still in use | `message` only, safe to show to the user |
| `422` | Business rule broken (e.g. price > MRP, two default variants) | `message` only, safe to show to the user |
| `500` | Unexpected | `message: "Something went wrong"` |

Validation error example (`400`):

```json
{ "code": 400, "message": "Validation failed",
  "data": { "name": "must not be blank", "sellingPrice": "must be greater than or equal to 0.00" } }
```

Nested fields use paths such as `variants[0].name` or `addons[1].price`.

### Conventions

| Thing | Format |
|---|---|
| Ids | `uuid` strings. Use these everywhere; numeric ids are never exposed. |
| Money | JSON **number**, 2 decimals (`70.00` → `70` in JS). Rupees. |
| Rates | number, e.g. `5.000` = 5% |
| Dates | ISO local date-time **without timezone**, e.g. `"2026-10-04T15:58:07.398863"` |
| Enums | upper-case strings (see types) |
| Lists | `GET /items` is paged (`page` is **0-based**). Categories and add-on groups return plain arrays. |
| Deletes | Soft delete. Deleted records disappear from every API (`404`). |

### Local development: use the Angular proxy (required)

The backend has **no CORS configuration**, so calling `http://localhost:8086` directly from
`http://localhost:4200` fails in the browser. Call relative URLs (`/api/...`) and proxy them:

`proxy.conf.json`
```json
{
  "/api": { "target": "http://localhost:8086", "secure": false, "changeOrigin": true }
}
```

`angular.json` → `projects.<app>.architect.serve.options`
```json
"proxyConfig": "proxy.conf.json"
```

In production, serve the UI and API under the same domain (reverse proxy), or CORS must be added to the backend.

---

## 2. TypeScript models

Save as `src/app/catalog/catalog.models.ts`.

```ts
// ---------- shared ----------
export interface ApiResponse<T> {
  code: number;
  message: string;
  data?: T;
}

export interface PageResult<T> {
  content: T[];
  page: number;          // 0-based
  size: number;
  totalElements: number;
  totalPages: number;
}

/** `data` of a 400 "Validation failed" response: field path -> message. */
export type ValidationErrors = Record<string, string>;

export type ItemType = 'GOODS' | 'SERVICE';
export type FoodType = 'VEG' | 'NON_VEG' | 'EGG';

// ---------- responses ----------
export interface Category {
  uuid: string;
  name: string;
  parentUuid: string | null;   // null = top-level
  imageUrl: string | null;
  sortOrder: number;
  active: boolean;
  itemCount: number;           // non-deleted items directly in this category
}

export interface Item {
  uuid: string;
  name: string;
  shortName: string | null;    // printed on small thermal bills
  itemCode: string | null;     // quick-type code, e.g. "101"
  barcode: string | null;
  categoryUuid: string | null;
  categoryName: string | null;
  itemType: ItemType;
  foodType: FoodType | null;   // null for non-food shops
  unitOfMeasure: string;       // upper-case: PCS, PLATE, KG, CUP ...
  sellingPrice: number;        // = default variant price when hasVariants
  mrp: number | null;
  priceIncludesTax: boolean;
  taxGroupUuid: string | null; // null = not taxed
  taxGroupName: string | null;
  taxRate: number | null;      // e.g. 5 for GST 5%
  hsnSacCode: string | null;
  hasVariants: boolean;
  openPrice: boolean;          // cashier types the price while billing
  favourite: boolean;          // show as quick button on billing screen
  imageUrl: string | null;
  description: string | null;
  sortOrder: number;
  active: boolean;             // false = "not available", cannot be billed
  updatedAt: string;           // ISO local date-time
  variants: ItemVariant[];     // [] when hasVariants = false
  addonGroupUuids: string[];   // resolve via the add-on groups list
}

export interface ItemVariant {
  uuid: string;
  name: string;                // Half / Full, 250g ...
  itemCode: string | null;
  barcode: string | null;
  sellingPrice: number;
  mrp: number | null;
  isDefault: boolean;          // exactly one per item
  sortOrder: number;
  active: boolean;
}

/** Result of a barcode scan / typed code. */
export interface ItemLookup {
  matchedVariantUuid: string | null; // set when the code belonged to a variant: pre-select it
  item: Item;
}

export interface AddonGroup {
  uuid: string;
  name: string;
  minSelect: number;           // customer must pick at least this many
  maxSelect: number | null;    // null = no upper limit
  active: boolean;
  addons: Addon[];
}

export interface Addon {
  uuid: string;
  name: string;
  price: number;
  foodType: FoodType | null;
  sortOrder: number;
  active: boolean;
}

// ---------- requests ----------
export interface CategoryRequest {
  name: string;                // required, max 255, unique per parent (case-insensitive)
  parentUuid?: string | null;  // must be a top-level category (max 2 levels)
  imageUrl?: string | null;    // max 500
  sortOrder?: number;          // default 0
  active?: boolean;            // default true (create always makes it active)
}

/** Used for both create (POST) and full replace (PUT). Send the WHOLE item on PUT. */
export interface ItemRequest {
  name: string;                // required, max 255
  shortName?: string | null;   // max 50
  itemCode?: string | null;    // max 50, unique across items AND variants of the shop
  barcode?: string | null;     // max 100, unique across items AND variants of the shop
  categoryUuid?: string | null;
  itemType?: ItemType;         // default GOODS
  foodType?: FoodType | null;
  unitOfMeasure?: string | null; // max 20, default PCS, stored upper-case
  hsnSacCode?: string | null;  // 4-8 digits
  imageUrl?: string | null;    // max 500
  description?: string | null;
  sortOrder?: number;          // default 0
  sellingPrice?: number | null; // required unless openPrice or variants given; ignored when variants given
  mrp?: number | null;         // sellingPrice must be <= mrp
  priceIncludesTax?: boolean;  // default true
  taxGroupUuid?: string | null;
  openPrice?: boolean;         // default false; cannot be combined with variants
  favourite?: boolean;         // default false
  active?: boolean;            // default true
  variants?: VariantRequest[]; // default []  (see "upsert lists" below)
  addonGroupUuids?: string[];  // default []  (order = display order)
}

export interface VariantRequest {
  uuid?: string | null;        // omit for NEW variant; send existing uuid to KEEP/UPDATE it
  name: string;                // required, max 100, unique within the item
  itemCode?: string | null;
  barcode?: string | null;
  sellingPrice: number;        // required, >= 0
  mrp?: number | null;
  isDefault?: boolean;         // max one true; if none, first active variant becomes default
  sortOrder?: number;
  active?: boolean;            // default true; the default variant must be active
}

export interface AddonGroupRequest {
  name: string;                // required, max 100, unique (case-insensitive)
  minSelect?: number;          // default 0; must be <= number of active add-ons
  maxSelect?: number | null;   // null = no limit; >= 1 and >= minSelect
  active?: boolean;            // default true (used on update)
  addons?: AddonRequest[];     // upsert list, same rules as variants
}

export interface AddonRequest {
  uuid?: string | null;
  name: string;                // required, max 100, unique within the group
  price?: number | null;       // default 0, >= 0
  foodType?: FoodType | null;
  sortOrder?: number;
  active?: boolean;            // default true
}

export interface ItemSearchParams {
  search?: string;             // name contains, or exact item/variant code or barcode
  categoryUuid?: string;
  active?: boolean;
  favourite?: boolean;
  foodType?: FoodType;
  page?: number;               // default 0
  size?: number;               // default 50, max 500
}
```

### Upsert lists (variants and add-ons)

`PUT` replaces the **whole** list:

| You send | Backend does |
|---|---|
| entry **with** `uuid` | updates that existing variant/add-on |
| entry **without** `uuid` | creates a new one |
| existing one **left out** | deletes it |

So when editing, start from the item you loaded and send every variant back **with its `uuid`**.
Sending an existing variant without its uuid deletes it and creates a new one with a new uuid.

---

## 3. Endpoints

All paths are relative to `/api/v1/catalog`. All responses use the `ApiResponse<T>` envelope; the table
lists `T`.

| Method | Path | Body | Returns `data` | Success |
|---|---|---|---|---|
| **Categories** |||||
| GET | `/categories?active=` | | `Category[]` | 200 |
| GET | `/categories/{uuid}` | | `Category` | 200 |
| POST | `/categories` | `CategoryRequest` | `Category` | 201 |
| PUT | `/categories/{uuid}` | `CategoryRequest` | `Category` | 200 |
| DELETE | `/categories/{uuid}` | | none | 200 |
| **Items** |||||
| GET | `/items?search=&categoryUuid=&active=&favourite=&foodType=&page=&size=` | | `PageResult<Item>` | 200 |
| GET | `/items/lookup?code=` | | `ItemLookup` | 200 |
| GET | `/items/{uuid}` | | `Item` | 200 |
| POST | `/items` | `ItemRequest` | `Item` | 201 |
| PUT | `/items/{uuid}` | `ItemRequest` (full) | `Item` | 200 |
| PATCH | `/items/{uuid}/active` | `{ "active": boolean }` | `Item` | 200 |
| PATCH | `/items/{uuid}/favourite` | `{ "favourite": boolean }` | `Item` | 200 |
| DELETE | `/items/{uuid}` | | none | 200 |
| **Add-on groups** |||||
| GET | `/addon-groups?active=` | | `AddonGroup[]` | 200 |
| GET | `/addon-groups/{uuid}` | | `AddonGroup` | 200 |
| POST | `/addon-groups` | `AddonGroupRequest` | `AddonGroup` | 201 |
| PUT | `/addon-groups/{uuid}` | `AddonGroupRequest` (full) | `AddonGroup` | 200 |
| DELETE | `/addon-groups/{uuid}` | | none | 200 |

Every write returns the **fresh record**, so the UI can replace its local copy without another GET.

### Categories

- `GET /categories` returns a **flat** list ordered by `sortOrder`, then name. Build the tree on the
  client with `parentUuid` (max 2 levels).
- **POST/PUT errors**: `404` parent not found · `409` name already exists under the same parent ·
  `422` parent is not top-level / own parent / a category with sub-categories can't be moved under another.
- **DELETE errors**: `409` has sub-categories · `409` has items.

### Items

- **Search** `search` matches: name *contains* (case-insensitive) **or** exact item code / barcode **or**
  exact variant code / barcode. Results ordered by `sortOrder`, then name.
- **Lookup** (`/items/lookup?code=...`) is for the billing screen's scan / code box. It finds only
  **active** items and **active** variants. If `matchedVariantUuid` is set, pre-select that variant.
  `404` = nothing billable with that code.
- **POST/PUT errors**:
  - `404` category, tax group or add-on group uuid not found
  - `409` item code / barcode already used by another item or variant (message names the code)
  - `422` examples: `Selling price cannot be more than MRP` · `Selling price is required` ·
    `Only one variant can be the default` · `Duplicate variant name: Half` · `The default variant must be active` ·
    `Open-price items cannot have variants` · `HSN/SAC code must be 4 to 8 digits` ·
    `Variant does not belong to this item: <uuid>`
- **PATCH active**: the "not available today" toggle. Inactive items still show in `GET /items`
  but cannot be found by lookup.

Create example:

```json
POST /api/v1/catalog/items
{
  "name": "Paneer Maggi",
  "itemCode": "105",
  "categoryUuid": "30000000-0000-0000-0000-000000000002",
  "foodType": "VEG",
  "unitOfMeasure": "plate",
  "taxGroupUuid": "20000000-0000-0000-0000-000000000002",
  "variants": [
    { "name": "Half", "itemCode": "105H", "sellingPrice": 55 },
    { "name": "Full", "itemCode": "105F", "sellingPrice": 90, "isDefault": true }
  ],
  "addonGroupUuids": ["40000000-0000-0000-0000-000000000001"]
}
```

Response shape: real output of `GET /items/{uuid}` for the seeded Veg Maggi. `POST` returns the same
shape with `"code": 201, "message": "Created"`.

```json
{
  "code": 200, "message": "OK",
  "data": {
    "uuid": "50000000-0000-0000-0000-000000000001", "name": "Veg Maggi", "shortName": "Veg Maggi",
    "itemCode": "101", "barcode": null,
    "categoryUuid": "30000000-0000-0000-0000-000000000002", "categoryName": "Maggi",
    "itemType": "GOODS", "foodType": "VEG", "unitOfMeasure": "PLATE",
    "sellingPrice": 70.00, "mrp": null, "priceIncludesTax": true,
    "taxGroupUuid": "20000000-0000-0000-0000-000000000002", "taxGroupName": "GST 5%", "taxRate": 5.000,
    "hsnSacCode": "996331", "hasVariants": true, "openPrice": false, "favourite": true,
    "imageUrl": null, "description": "Classic masala maggi with veggies",
    "sortOrder": 1, "active": true, "updatedAt": "2026-10-04T15:58:07.398863",
    "variants": [
      { "uuid": "51000000-0000-0000-0000-000000000001", "name": "Half", "itemCode": "101H", "barcode": null,
        "sellingPrice": 40.00, "mrp": null, "isDefault": false, "sortOrder": 1, "active": true },
      { "uuid": "51000000-0000-0000-0000-000000000002", "name": "Full", "itemCode": "101F", "barcode": null,
        "sellingPrice": 70.00, "mrp": null, "isDefault": true, "sortOrder": 2, "active": true }
    ],
    "addonGroupUuids": ["40000000-0000-0000-0000-000000000001", "40000000-0000-0000-0000-000000000002"]
  }
}
```

### Add-on groups

- Each group's `addons` includes **inactive** add-ons. Filter by `active` on the billing screen.
- When billing, enforce `minSelect` / `maxSelect` per group (e.g. Spice Level: exactly 1).
- **POST/PUT errors**: `409` name exists · `422` `Maximum selection cannot be less than minimum` ·
  `Minimum selection (3) is more than the active add-ons (1)` · `Duplicate add-on name: ...`
- **DELETE errors**: `409` still attached to items (remove it from those items first).

---

## 4. Billing-screen recipe

1. On load: `GET /categories?active=true`, `GET /addon-groups?active=true`, and
   `GET /items?active=true&size=500` (page through if `totalPages > 1`). Cache them.
2. Quick buttons: items with `favourite === true`.
3. Scan / type code → `GET /items/lookup?code=...` → add item (pre-select `matchedVariantUuid`).
4. If `hasVariants`, ask for a variant (default = `isDefault`). Price = variant `sellingPrice`.
5. For each uuid in `addonGroupUuids`, show that group's **active** add-ons and enforce min/max.
6. If `openPrice`, ask the cashier for the price.

---

## 5. Angular service

Save as `src/app/catalog/catalog.service.ts`. The service unwraps the envelope and returns plain models.

```ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  AddonGroup, AddonGroupRequest, ApiResponse, Category, CategoryRequest,
  Item, ItemLookup, ItemRequest, ItemSearchParams, PageResult,
} from './catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/catalog'; // relative: goes through proxy.conf.json in dev

  // ---------- categories ----------
  listCategories(active?: boolean): Observable<Category[]> {
    return this.get<Category[]>('/categories', { active });
  }
  getCategory(uuid: string): Observable<Category> {
    return this.get<Category>(`/categories/${uuid}`);
  }
  createCategory(body: CategoryRequest): Observable<Category> {
    return this.unwrap(this.http.post<ApiResponse<Category>>(`${this.base}/categories`, body));
  }
  updateCategory(uuid: string, body: CategoryRequest): Observable<Category> {
    return this.unwrap(this.http.put<ApiResponse<Category>>(`${this.base}/categories/${uuid}`, body));
  }
  deleteCategory(uuid: string): Observable<void> {
    return this.delete(`/categories/${uuid}`);
  }

  // ---------- items ----------
  searchItems(params: ItemSearchParams = {}): Observable<PageResult<Item>> {
    return this.get<PageResult<Item>>('/items', { ...params });
  }
  lookupItem(code: string): Observable<ItemLookup> {
    return this.get<ItemLookup>('/items/lookup', { code });
  }
  getItem(uuid: string): Observable<Item> {
    return this.get<Item>(`/items/${uuid}`);
  }
  createItem(body: ItemRequest): Observable<Item> {
    return this.unwrap(this.http.post<ApiResponse<Item>>(`${this.base}/items`, body));
  }
  /** Full replace: send the complete item, including every variant you want to keep (with uuid). */
  updateItem(uuid: string, body: ItemRequest): Observable<Item> {
    return this.unwrap(this.http.put<ApiResponse<Item>>(`${this.base}/items/${uuid}`, body));
  }
  setItemActive(uuid: string, active: boolean): Observable<Item> {
    return this.unwrap(this.http.patch<ApiResponse<Item>>(`${this.base}/items/${uuid}/active`, { active }));
  }
  setItemFavourite(uuid: string, favourite: boolean): Observable<Item> {
    return this.unwrap(this.http.patch<ApiResponse<Item>>(`${this.base}/items/${uuid}/favourite`, { favourite }));
  }
  deleteItem(uuid: string): Observable<void> {
    return this.delete(`/items/${uuid}`);
  }

  // ---------- add-on groups ----------
  listAddonGroups(active?: boolean): Observable<AddonGroup[]> {
    return this.get<AddonGroup[]>('/addon-groups', { active });
  }
  getAddonGroup(uuid: string): Observable<AddonGroup> {
    return this.get<AddonGroup>(`/addon-groups/${uuid}`);
  }
  createAddonGroup(body: AddonGroupRequest): Observable<AddonGroup> {
    return this.unwrap(this.http.post<ApiResponse<AddonGroup>>(`${this.base}/addon-groups`, body));
  }
  updateAddonGroup(uuid: string, body: AddonGroupRequest): Observable<AddonGroup> {
    return this.unwrap(this.http.put<ApiResponse<AddonGroup>>(`${this.base}/addon-groups/${uuid}`, body));
  }
  deleteAddonGroup(uuid: string): Observable<void> {
    return this.delete(`/addon-groups/${uuid}`);
  }

  // ---------- helpers ----------
  private get<T>(path: string, query: Record<string, string | number | boolean | undefined | null> = {}): Observable<T> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.unwrap(this.http.get<ApiResponse<T>>(`${this.base}${path}`, { params }));
  }

  private delete(path: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}${path}`).pipe(map(() => undefined));
  }

  private unwrap<T>(request: Observable<ApiResponse<T>>): Observable<T> {
    return request.pipe(map(res => res.data as T));
  }
}
```

### Auth

No POS-specific interceptor is needed: the kubee-auth interceptor that already adds
`Authorization: Bearer <token>` (ez-inventory-ui `layouts/interceptors/auth.interceptor.ts`) covers the POS API too.
The backend reads the tenant from the token, so the app never sends a tenant id itself.

### Showing errors

```ts
import { HttpErrorResponse } from '@angular/common/http';
import { ApiResponse, ValidationErrors } from './catalog.models';

export function readApiError(err: HttpErrorResponse): { message: string; fields: ValidationErrors } {
  const body = err.error as ApiResponse<ValidationErrors> | null;
  return {
    message: body?.message ?? 'Network error',
    fields: err.status === 400 && body?.data ? body.data : {},
  };
}
```

- `400` → put `fields[name]` under each form control (paths like `variants[0].name` for nested rows).
- `409` / `422` → show `message` as a toast or form-level error; the text is user-friendly.
- `404` on GET → record was deleted or belongs to another shop; go back to the list.

---

## 6. Test data

Load `db/seed/catalog_seed.sql` (see that file's header) and log in as a user whose token's `tenantUuid` is:

- `10000000-0000-0000-0000-000000000001`: **Maggi Point** (food counter: variants, add-ons, favourites)
- `10000000-0000-0000-0000-000000000002`: **Sri Balaji Kirana** (grocery: barcodes, MRP)

Handy codes for the lookup box (tenant 1): `101` Veg Maggi · `101H` Veg Maggi (Half) ·
`2000000000014` Cola 300ml (barcode) · `203` inactive (`404`) · `999` open-price item.
