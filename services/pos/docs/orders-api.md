# Orders & Payments API: Frontend Guide (Angular)

Everything the billing screen needs to take an order and get paid: endpoints, request/response models as
TypeScript types, the order lifecycle and the rules the server enforces. Shapes are taken from real responses.

- Backend: Kubee POS (`pos`), default port **8086**
- Base path: **`/api/v1/orders`**
- Covers: **Orders** (lines, add-ons, discounts, GST, round-off, hold/recall, cancel) and **Payments**
  (cash with change, UPI, card, split, refunds)
- Headers, response envelope, errors, conventions and the Angular proxy are the same as the catalog:
  see [`catalog-api.md`](catalog-api.md) section 1.

---

## 1. How an order works

```
             ┌──── hold ────┐
   create ─► OPEN ◄─ recall ─ HELD
             │  add / change / remove lines, discounts, payments
             ├─ last payment (nothing due) ──► COMPLETED ── refunds allowed
             └─ cancel (only when nothing is paid) ──► CANCELLED
```

- **Every endpoint returns the whole order** (`OrderView`). Just re-render the screen from the response.
- Lines, discounts and payments can only change while the order is **`OPEN`**. A `HELD` order must be recalled first.
- **The order completes itself** when a payment brings `dueAmount` to 0. `POST /complete` is only for an order
  whose total is 0.
- **Cancel** needs a reason and works only when `paidAmount` is 0. To cancel an order that was paid, refund it first.
  If the order has an issued GST bill, cancel the bill first ([`billing-api.md`](billing-api.md)).
- **Refunds** work on any order that isn't cancelled (including completed ones). Each refund points at one payment.
- `orderNumber` is the token for the customer ("23"). It restarts at 1 every day, per shop.
- `dueAmount` is always 0 on completed and cancelled orders.
- **Online orders (Zomato / Swiggy)**: the shop accepts the order on the aggregator's partner app and enters it
  here with `source: 'ZOMATO' | 'SWIGGY' | 'OTHER'` and the aggregator's id in `externalOrderId`. The customer
  already paid the aggregator, so take the payment with `method: 'AGGREGATOR'` (only allowed on online orders).
  The same `externalOrderId` can't be entered twice for a source unless the earlier order was cancelled (`409`).

### Money, worked out by the server

Never compute totals on the client for anything you send. Show what the server returns.

1. `lineAmount` = (`unitPrice` + `addonsUnitPrice`) × `quantity`
2. Line discount (`discountType`/`discountValue` on the line) comes off the line.
3. The bill discount (`PERCENT` or `FLAT`) is spread over the lines in proportion to their value.
   `discountAmount` on a line = its own discount + its share of the bill discount.
4. GST per line at the item's tax rate. If `priceIncludesTax` is true, tax is taken out of the price
   (₹105 at 5% = ₹100 taxable + ₹5 tax). Otherwise tax is added on top.
5. `roundOffAmount` makes the total a round figure using the shop's rule (default: nearest rupee).
   It can be negative.
6. `grandTotal` = sum of line `totalAmount` + `roundOffAmount`. `dueAmount` = `grandTotal` − `paidAmount`.

Name, price and tax on a line are **snapshots** taken when the item was added. Later catalog changes never alter it.

---

## 2. TypeScript models

```ts
export type OrderType = 'COUNTER' | 'TAKEAWAY' | 'DINE_IN' | 'DELIVERY';
export type OrderStatus = 'OPEN' | 'HELD' | 'COMPLETED' | 'CANCELLED';
export type PaymentStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'PARTIALLY_REFUNDED' | 'REFUNDED';
export type OrderSource = 'POS' | 'ZOMATO' | 'SWIGGY' | 'OTHER';
export type PaymentMethod = 'CASH' | 'UPI' | 'CARD' | 'WALLET' | 'BANK_TRANSFER' | 'OTHER' | 'AGGREGATOR';
export type PaymentType = 'PAYMENT' | 'REFUND';
export type TransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
export type DiscountType = 'PERCENT' | 'FLAT';

export interface OrderView {
  uuid: string;
  orderNumber: string;            // token for the customer, daily
  clientRef: string | null;
  orderType: OrderType;
  source: OrderSource;            // POS = taken at the counter
  externalOrderId: string | null; // Zomato / Swiggy order id
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  customerName: string | null;
  customerPhone: string | null;   // digits only, e.g. "9848022338"
  tableLabel: string | null;
  notes: string | null;
  subTotal: number;               // before any discount
  discountType: DiscountType | null;   // the bill discount as entered
  discountValue: number | null;
  discountAmount: number;         // all discounts in rupees (line + bill)
  discountReason: string | null;
  taxableAmount: number;
  taxAmount: number;
  roundOffAmount: number;         // may be negative
  grandTotal: number;
  paidAmount: number;             // payments − refunds
  dueAmount: number;
  createdBy: string | null;       // user uuid
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancelReason: string | null;
  lines: OrderLineView[];
  payments: PaymentView[];
}

export interface OrderLineView {
  uuid: string;
  itemUuid: string | null;        // null if the item was deleted since
  variantUuid: string | null;
  itemName: string;
  variantName: string | null;     // "Medium (10\")"
  hsnSacCode: string | null;
  unitOfMeasure: string;
  quantity: number;               // up to 3 decimals
  unitPrice: number;
  addonsUnitPrice: number;        // add-ons per unit of the line
  priceIncludesTax: boolean;
  lineAmount: number;
  discountType: DiscountType | null;   // the line discount as entered
  discountValue: number | null;
  discountAmount: number;         // line discount + share of bill discount
  taxRate: number;                // 5.000 = 5%
  taxableAmount: number;
  taxAmount: number;
  totalAmount: number;
  notes: string | null;           // "less spicy"
  addons: OrderLineAddonView[];
}

export interface OrderLineAddonView {
  uuid: string;
  addonUuid: string | null;
  name: string;
  quantity: number;               // per unit of the line
  unitPrice: number;
}

export interface PaymentView {
  uuid: string;
  clientRef: string | null;
  type: PaymentType;
  method: PaymentMethod;
  status: TransactionStatus;
  amount: number;
  tenderedAmount: number | null;  // cash handed over
  changeAmount: number | null;    // cash to give back
  refundableAmount: number | null;// PAYMENT rows: how much can still be refunded
  referenceNo: string | null;     // UPI UTR / card approval code
  refundOfPaymentUuid: string | null;  // REFUND rows
  notes: string | null;
  paidAt: string;
  receivedBy: string | null;
}

export interface OrderSummaryView {
  uuid: string;
  orderNumber: string;
  orderType: OrderType;
  source: OrderSource;            // POS = taken at the counter
  externalOrderId: string | null; // Zomato / Swiggy order id
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  customerName: string | null;
  customerPhone: string | null;
  tableLabel: string | null;
  lineCount: number;
  grandTotal: number;
  paidAmount: number;
  dueAmount: number;
  createdAt: string;
  completedAt: string | null;
}

// ---------------------------------------------------------------- requests

export interface OrderLineRequest {
  itemUuid: string;
  variantUuid?: string;           // required when the item hasVariants
  quantity?: number;              // default 1
  unitPrice?: number;             // ONLY for openPrice items
  addons?: { addonUuid: string; quantity?: number }[];
  discountType?: DiscountType;
  discountValue?: number;
  notes?: string;
}

export interface CreateOrderRequest {
  clientRef?: string;             // uuid made on the device: makes the call safe to retry
  orderType?: OrderType;          // default COUNTER
  customerName?: string;
  customerPhone?: string;
  tableLabel?: string;
  notes?: string;
  source?: OrderSource;           // default POS
  externalOrderId?: string;       // online orders only, max 64
  lines?: OrderLineRequest[];
  discount?: DiscountRequest;
}

export interface ChangeOrderLineRequest {   // full replace: omitted discount / note = removed
  quantity: number;
  discountType?: DiscountType;
  discountValue?: number;
  notes?: string;
}

export interface DiscountRequest { type: DiscountType; value: number; reason?: string; }

export interface OrderDetailsRequest {      // full replace: omitted fields are cleared
  orderType?: OrderType;
  customerName?: string;
  customerPhone?: string;
  tableLabel?: string;
  notes?: string;
  source?: OrderSource;           // left out = unchanged
  externalOrderId?: string;       // left out = cleared
}

export interface PaymentRequest {
  clientRef?: string;             // uuid made on the device: a retry is ignored, never double-charged
  method: PaymentMethod;
  amount?: number;                // required except for CASH with tenderedAmount
  tenderedAmount?: number;        // CASH only
  referenceNo?: string;
  notes?: string;
}

export interface RefundRequest {
  clientRef?: string;
  amount: number;
  method?: PaymentMethod;         // default: same as the original payment
  reason: string;
}
```

---

## 3. Endpoints

All responses are `{ code, message, data }`. Unless noted, `data` is the updated `OrderView`.

### Orders

| Method | Path | Body | Notes |
|---|---|---|---|
| `POST` | `/api/v1/orders` | `CreateOrderRequest` (or empty) | `201`. Lines and discount are optional. Same `clientRef` again → same order. |
| `GET` | `/api/v1/orders` | | Paged `OrderSummaryView`, newest first. Query: `status`, `paymentStatus`, `orderType`, `source`, `from`, `to` (`yyyy-MM-dd`, inclusive), `search` (order number, online order id, phone, name), `page` (0-based), `size` (default 50, max 200) |
| `GET` | `/api/v1/orders/{uuid}` | | One order |
| `PUT` | `/api/v1/orders/{uuid}/details` | `OrderDetailsRequest` | Customer, type, table, notes. Allowed until cancelled (e.g. add a phone after paying). |
| `POST` | `/api/v1/orders/{uuid}/lines` | `OrderLineRequest` | Scanning the same plain item again (no add-ons, discount or note) **adds to the quantity** of the existing line |
| `PATCH` | `/api/v1/orders/{uuid}/lines/{lineUuid}` | `ChangeOrderLineRequest` | To change the item, variant or add-ons: remove the line and add it again |
| `DELETE` | `/api/v1/orders/{uuid}/lines/{lineUuid}` | | |
| `PUT` | `/api/v1/orders/{uuid}/discount` | `DiscountRequest` | Bill discount, on top of line discounts |
| `DELETE` | `/api/v1/orders/{uuid}/discount` | | |
| `POST` | `/api/v1/orders/{uuid}/hold` | | Park it. Needs at least one line. |
| `POST` | `/api/v1/orders/{uuid}/recall` | | `HELD` → `OPEN` |
| `POST` | `/api/v1/orders/{uuid}/complete` | | Only when nothing is due |
| `POST` | `/api/v1/orders/{uuid}/cancel` | `{ "reason": "..." }` | Only when nothing is paid |

Held orders list: `GET /api/v1/orders?status=HELD`. Today's orders: `GET /api/v1/orders?from=2026-10-05&to=2026-10-05`.

### Payments

| Method | Path | Body | Notes |
|---|---|---|---|
| `POST` | `/api/v1/orders/{uuid}/payments` | `PaymentRequest` | Split payment = one call per method. The order completes on the last one. |
| `POST` | `/api/v1/orders/{uuid}/payments/{paymentUuid}/refund` | `RefundRequest` | Up to the payment's `refundableAmount`. **Manager only** (staff get `403`). |

Cash example: due ₹380, customer gives ₹500 → `{"method":"CASH","tenderedAmount":500}` → payment `amount: 380`,
`changeAmount: 120`, order `COMPLETED`.

### Errors you will see

| Status | Example `message` | What to do |
|---|---|---|
| `400` | `Validation failed` (with `data: { field: error }`) | Fix the form field |
| `404` | `Order not found: …`, `Item not found: …` | Reload |
| `409` | `This was changed on another device. Reload and try again.` | Two tills touched the same order at once. Reload it (`GET`) and retry. |
| `409` | `Conflicts with existing data` | A `clientRef` already used by a *different* order or payment |
| `409` | `ZOMATO order 5512345 is already entered as order #12` | The online order was entered before |
| `422` | `Online order id is only for Zomato / Swiggy orders` / `Aggregator payment is only for Zomato / Swiggy orders` | |
| `422` | `Pick a variant of Margherita Pizza` | Ask for a variant |
| `422` | `Choose at least 1 from Crust` / `Choose at most 1 from Spice Level` | Add-on group min/max |
| `422` | `Paneer Burger is not available right now` | Item or variant inactive |
| `422` | `Enter the price for Open Item` / `Price can only be entered for open-price items` | |
| `422` | `This order is on hold. Recall it before changing it.` | |
| `422` | `Amount 500.00 is more than the 137.00 due` | |
| `422` | `300.00 has been paid on this order. Refund it before cancelling.` | |
| `422` | `Order total 90.00 cannot be less than the 100.00 already paid. Refund first.` | Removing or discounting after a part payment |
| `422` | `Cancel bill KPA/26-27/000001 before cancelling this order` | The order has an issued GST bill |

All `422` messages are written for the cashier: show them as they are.

---

## 4. Billing-screen recipe

1. **New sale**: `POST /orders` with `{ clientRef: crypto.randomUUID() }` (or wait for the first item and send it in `lines`).
2. **Scan / type code**: `GET /catalog/items/lookup?code=…`, then `POST /orders/{uuid}/lines` with `itemUuid`
   (+ `variantUuid` = `matchedVariantUuid` when set). For items with add-on groups, collect choices first.
3. **Qty +/−, line discount, note**: `PATCH /lines/{lineUuid}`. **Remove**: `DELETE /lines/{lineUuid}`.
4. **Bill discount**: `PUT /discount`.
5. **Customer went to fetch something**: `POST /hold`, then start a new sale. Show `GET /orders?status=HELD`
   as a "held bills" list. Picking one: `POST /recall`.
6. **Pay**:
   - `F2` cash: ask for the tendered amount → `{ method: 'CASH', tenderedAmount }` → show `changeAmount`.
   - `F3` UPI: show the shop QR for `dueAmount`, cashier confirms → `{ method: 'UPI', amount: dueAmount, referenceNo? }`.
   - Split: pay part by UPI, then the rest by cash. Watch `dueAmount`.
   - Always send a fresh `clientRef` per payment, so a double tap or retry never charges twice.
7. When `status === 'COMPLETED'`: issue the bill (`POST /api/v1/bills` with `{ orderUuid }`, see
   [`billing-api.md`](billing-api.md)), print / share it, then start a new sale.

Refund screen: open the order, pick a `PAYMENT` row with `refundableAmount > 0`, ask amount + reason →
`POST /payments/{paymentUuid}/refund`.

---

## 5. Not built yet

- **Dynamic UPI QR with auto-confirm** (payment gateway + webhook). `TransactionStatus.PENDING` is reserved for it.
- **Discount limits per role**: refunds are manager-only (see [`catalog-api.md`](catalog-api.md) Permissions),
  but any staff member can give a discount of any size.
- **Audit log** of every change (who / when / what). Today an order records who created it, who cancelled it
  and who took each payment.
