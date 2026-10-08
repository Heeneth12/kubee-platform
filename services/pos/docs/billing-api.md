# Billing (GST Invoice) API: Frontend Guide (Angular)

How to issue, show, print, share and cancel the GST invoice for an order. Shapes are taken from real responses.

- Backend: Kubee POS (`pos`), default port **8086**
- Base path: **`/api/v1/bills`** (plus `GET /api/v1/orders/{orderUuid}/bill`)
- Headers, response envelope, errors and conventions: same as the catalog, see [`catalog-api.md`](catalog-api.md) section 1.
- Orders and payments: [`orders-api.md`](orders-api.md)

---

## 1. How bills work

- A bill is issued for a **completed** (fully paid) order: take payment → issue bill → print / share.
- **Calling issue again returns the same bill** (safe on double tap / retry). Asking for *different* customer
  details while a bill exists gives `409`: cancel it first.
- **An issued bill never changes.** Seller, customer, lines, tax split and totals are copied onto it.
  Later changes to settings, catalog or the order never touch it. To correct a bill (wrong GSTIN, customer
  wanted a B2B bill), **cancel it and issue a new one**. The new one gets the next number.
- An order with an issued bill **cannot be cancelled**. Cancel the bill first. A fully refunded order can't be billed.
- **Bill number**: `<bill prefix><series>/<yy-yy>/<6 digits>`, e.g. `KPA/26-27/000123`.
  - The counter is per **series** and per **financial year** (April–March by default; set in shop settings). No gaps:
    a failed issue does not use up a number.
  - `series` (1–4 letters/digits, default `A`): give each counter / device its own, e.g. `A`, `B`, so offline
    devices never clash later.
  - GST allows at most 16 characters, so `bill prefix + series` can be at most 6 characters.
- **Tax split** per line:
  - Customer in the **same state** as the shop (or no state given) → **CGST + SGST** (half each).
  - **Other state** → **IGST** at the full rate. The customer's state is `placeOfSupply`; it defaults to the
    first 2 digits of `customerGstin`.
  - The split uses the item's tax group components; CESS, if configured, is added on top.
  - The parts always add up to the line's tax exactly (the last part takes any odd paisa).
- Shop details come from shop settings (`pos_settings`). Without a shop name, issuing fails with `422`.

---

## 2. TypeScript models

```ts
export type BillStatus = 'ISSUED' | 'CANCELLED';
export type TaxType = 'CGST' | 'SGST' | 'IGST' | 'CESS' | 'OTHER';
export type ShareChannel = 'PRINT' | 'WHATSAPP' | 'SMS';

export interface BillView {
  uuid: string;
  billNumber: string;             // "KPA/26-27/000001"
  billDate: string;
  status: BillStatus;
  orderUuid: string;
  orderNumber: string;            // token on the order
  seller: { name: string; address: string | null; gstin: string | null; stateCode: string | null };
  buyer: { name: string | null; phone: string | null; gstin: string | null; placeOfSupply: string | null };
  interState: boolean;            // true = IGST bill
  subTotal: number;
  discountAmount: number;
  taxableAmount: number;
  taxAmount: number;
  roundOffAmount: number;
  grandTotal: number;
  amountInWords: string;          // "Rupees Four Hundred Eighty Five Only"
  lines: BillLineView[];
  taxSummary: TaxSummaryView[];   // the tax table at the bottom of the invoice
  hsnSummary: HsnSummaryView[];   // HSN/SAC-wise summary
  printCount: number;
  sharedVia: ShareChannel | null; // last way it reached the customer
  issuedBy: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancelReason: string | null;
}

export interface BillLineView {
  uuid: string;
  itemName: string;               // "Margherita Pizza (Medium (10\"))" = item (variant)
  addonsText: string | null;      // "+ Thin Crust, Extra Cheese"
  hsnSacCode: string | null;
  unitOfMeasure: string;
  quantity: number;
  unitPrice: number;              // including add-ons
  lineAmount: number;
  discountAmount: number;
  taxRate: number;
  taxableAmount: number;
  taxAmount: number;
  totalAmount: number;
  taxes: { taxType: TaxType; taxName: string; rate: number; taxableAmount: number; taxAmount: number }[];
}

export interface TaxSummaryView { taxType: TaxType; rate: number; taxableAmount: number; taxAmount: number; }

export interface HsnSummaryView {
  hsnSacCode: string | null; taxRate: number; quantity: number;
  taxableAmount: number; taxAmount: number; totalAmount: number;
}

export interface BillSummaryView {
  uuid: string; billNumber: string; billDate: string; status: BillStatus;
  orderUuid: string; orderNumber: string;
  customerName: string | null; customerPhone: string | null; customerGstin: string | null;
  taxableAmount: number; taxAmount: number; grandTotal: number; printCount: number;
}

export interface IssueBillRequest {
  orderUuid: string;
  series?: string;                // default "A"
  customerName?: string;          // default: from the order
  customerPhone?: string;         // default: from the order
  customerGstin?: string;         // B2B only
  placeOfSupply?: string;         // 2-digit state code; default: GSTIN's state, else the shop's
}
```

---

## 3. Endpoints

All responses are `{ code, message, data }`. Unless noted, `data` is a `BillView`.

| Method | Path | Body | Notes |
|---|---|---|---|
| `POST` | `/api/v1/bills` | `IssueBillRequest` | `201`. Same order again → same bill. |
| `GET` | `/api/v1/bills/{uuid}` | | One bill, with lines, tax split, summaries, amount in words |
| `GET` | `/api/v1/orders/{orderUuid}/bill` | | The order's current (issued) bill; `404` if none |
| `GET` | `/api/v1/bills` | | Paged `BillSummaryView`, newest first. Query: `status`, `from`, `to` (`yyyy-MM-dd`), `search` (bill number, order number, phone, GSTIN), `page`, `size` |
| `POST` | `/api/v1/bills/{uuid}/print` | | Call after printing / reprinting; counts prints |
| `POST` | `/api/v1/bills/{uuid}/share` | `{ "channel": "WHATSAPP" }` | Records how it was sent (the app sends it for now) |
| `POST` | `/api/v1/bills/{uuid}/cancel` | `{ "reason": "..." }` | Then issue a new one if needed. **Manager only** (staff get `403`). |

### Errors you will see

| Status | `message` | Meaning |
|---|---|---|
| `422` | `Only a completed (fully paid) order can be billed (this one is OPEN)` | Take payment first |
| `422` | `This order was fully refunded; there is nothing to bill` | |
| `422` | `Add the shop name and address in settings before issuing bills` | No shop settings |
| `422` | `Customer GSTIN is not a valid GSTIN` | 15 chars, e.g. `29ABCDE1234F1Z5` |
| `422` | `Bill number … is longer than 16 characters…` | Shorten the bill prefix / series |
| `409` | `Order already has bill KPA/26-27/000002. Cancel it to issue a new one with different customer details.` | |
| `422` | `This bill is already cancelled` | |
| `422` | `Cancel bill KPA/26-27/000001 before cancelling this order` | From `POST /orders/{uuid}/cancel` |

---

## 4. Screen recipe

1. When the order response has `status === 'COMPLETED'` → `POST /bills` with `{ orderUuid }`.
2. Print from the `BillView` (58/80 mm CSS template), then `POST /bills/{uuid}/print`.
   - **Header**: `seller.name`, `seller.address`, `GSTIN: seller.gstin`, "Tax Invoice", `billNumber`, `billDate`, token `orderNumber`.
   - **Customer**: `buyer.*` (show `GSTIN` and place of supply only when present).
   - **Lines**: `itemName`, `addonsText` on the next line, `quantity × unitPrice`, `totalAmount`.
   - **Totals**: `subTotal`, `−discountAmount`, `taxableAmount`, the `taxSummary` rows (e.g. `CGST 2.5%  9.83`),
     `roundOffAmount`, **`grandTotal`**, `amountInWords`.
   - Optional: `hsnSummary` table (needed for A4 / B2B invoices).
   - If `status === 'CANCELLED'` print a big "CANCELLED".
3. Reprint later: find it with `GET /bills?search=…` or `GET /orders/{orderUuid}/bill`, print, call `/print` again.
4. Customer wants a GST bill after paying: `POST /bills/{uuid}/cancel` (reason "B2B bill requested") →
   `POST /bills` with `customerGstin` and name.

---

## 5. Not built yet

- PDF generation / `pdfUrl`, and actually sending on WhatsApp or SMS (only recorded).
- **Credit notes**: refunds after a bill is issued don't adjust the bill. Today you either keep the bill or
  cancel it and issue a new one.
- **Bill of supply**: shops without a GSTIN (or composition dealers) shouldn't charge GST. The bill is still
  issued with the tax from the order.
- GST reports (GSTR-1 / tax summary by date range): the data is in `bill_item_taxes`; the report endpoints come next.
