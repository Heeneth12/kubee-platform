# Reports API: Frontend Guide (Angular)

Day sales, payment modes, item-wise, category-wise, hourly and staff-wise sales, GST summary (for the CA),
cancellations/refunds and cash-drawer shifts. Every report can be downloaded as CSV or Excel, and GST as a
GSTR-1 JSON file. All read-only and **manager-only** (staff get `403`, see Permissions in
[`catalog-api.md`](catalog-api.md)).

- Backend: Kubee POS (`pos`), default port **8086**
- Base path: **`/api/v1/reports`**
- Headers, response envelope, errors and conventions: same as the catalog, see [`catalog-api.md`](catalog-api.md) section 1.

---

## 1. Basics

Every report takes the same period:

| Param | Format | Default |
|---|---|---|
| `from` | `yyyy-MM-dd`, inclusive | today (India) |
| `to` | `yyyy-MM-dd`, inclusive | today |

- `GET /api/v1/reports/sales-summary` with no params = today's day-end summary.
- At most **366 days** (a full financial year fits). `from` after `to`, or a longer range → `422`.
- Days run midnight to midnight, shop time.

**Which events count, and when.** Each figure is counted on the day it happened:

| Figure | Counted from | Dated by |
|---|---|---|
| Sales, item-wise | orders that are `COMPLETED` (fully paid) | completion time |
| Collected / refunded, payment modes | successful payments and refunds | payment time |
| GST | bills with status `ISSUED` (cancelled bills excluded) | bill date |
| Cancellations | cancelled orders / bills, refunds | cancel / refund time |

So an order paid today but completed yesterday shows in yesterday's sales and today's collections. Open and
held orders are never sales.

---

## 2. Endpoints and models

### 2.1 Day sales: `GET /reports/sales-summary?from&to`

```ts
export interface SalesSummaryReport {
  from: string; to: string;
  completedOrders: number;
  grossSales: number;        // before discounts
  discountAmount: number;
  taxableAmount: number;
  taxAmount: number;
  roundOffAmount: number;
  netSales: number;          // what customers were charged (sum of grand totals)
  averageOrderValue: number;
  collected: number;         // payments received in the period
  refunded: number;          // refunds given in the period
  netCollected: number;      // collected − refunded = money that should be in the drawer/bank
  cancelledOrders: number;
  billsIssued: number;       // bills dated in the period (incl. ones cancelled later)
  billsCancelled: number;    // bills cancelled in the period
  days: { date: string; orders: number; netSales: number; taxAmount: number; averageOrderValue: number }[];
}
```

`days` has one row per day that had sales, for a daily chart on longer ranges.

### 2.2 Payment modes: `GET /reports/payment-modes?from&to`

```ts
export interface PaymentModeRow {
  method: 'CASH' | 'UPI' | 'CARD' | 'WALLET' | 'BANK_TRANSFER' | 'OTHER' | null;  // null on the total row
  paymentCount: number; paymentAmount: number;
  refundCount: number; refundAmount: number;
  netAmount: number;
}
export interface PaymentModeReport { from: string; to: string; methods: PaymentModeRow[]; total: PaymentModeRow; }
```

`methods` is sorted by amount. The CASH `netAmount` is what the cash drawer should hold from sales.

### 2.3 Item-wise sales: `GET /reports/items?from&to&categoryUuid&sort`

- `sort`: `AMOUNT` (default, best sellers by value) or `QUANTITY`
- `categoryUuid`: only items currently in that category (sub-category uuid for e.g. "Hot Drinks")

```ts
export interface ItemSalesRow {
  itemUuid: string | null; itemName: string;
  variantUuid: string | null; variantName: string | null;   // one row per item + variant
  categoryName: string | null;
  unitOfMeasure: string;
  quantity: number;
  orderCount: number;        // orders it appeared in
  grossAmount: number; discountAmount: number; taxableAmount: number; taxAmount: number;
  netAmount: number;         // after discounts, incl. GST
  shareOfSales: number;      // % of totalNetAmount
}
export interface ItemSalesReport { from: string; to: string; totalNetAmount: number; items: ItemSalesRow[]; }
```

`totalNetAmount` excludes the order round-off, so it can differ from `netSales` by a few rupees. Refunds are
recorded per payment, not per item, so they aren't taken off here.

### 2.4 GST summary: `GET /reports/gst?from&to`

Laid out like GSTR-1, for the shop's CA. Built from **issued** bills only.

```ts
export interface GstReport {
  from: string; to: string;
  totals: {
    invoiceCount: number; taxableAmount: number;
    cgst: number; sgst: number; igst: number; cess: number; totalTax: number;
    roundOff: number; invoiceValue: number;          // invoice value = bill totals incl. round-off
  };
  byRate: { rate: number; taxableAmount: number; cgst: number; sgst: number; igst: number; cess: number;
            totalTax: number; invoiceValue: number }[];          // invoiceValue here excludes round-off
  b2bInvoices: { billNumber: string; billDate: string; customerGstin: string; customerName: string | null;
                 placeOfSupply: string; taxableAmount: number; cgst: number; sgst: number; igst: number;
                 cess: number; invoiceValue: number }[];         // one row per B2B invoice
  b2c: { placeOfSupply: string | null; rate: number; taxableAmount: number;
         cgst: number; sgst: number; igst: number; cess: number }[];   // unregistered buyers, by state + rate
  hsn: { hsnSacCode: string | null; unitOfMeasure: string; rate: number; quantity: number; taxableAmount: number;
         cgst: number; sgst: number; igst: number; cess: number; totalValue: number }[];
  ordersWithoutBill: number;   // completed orders in the period with no issued bill: their tax is NOT in here
  cancelledBills: number;      // bills dated in the period that were cancelled (excluded above)
}
```

- `byRate`, `hsn` and `b2bInvoices` + `b2c` each add up to the `totals`.
- **Show a warning when `ordersWithoutBill > 0`**: GST was charged on those orders but no invoice was issued.
- `hsn` rows with `hsnSacCode: null` are items with no HSN code in the catalog. The CA will need one.

### 2.5 Cancellations & refunds: `GET /reports/cancellations?from&to`

```ts
export interface CancellationReport {
  from: string; to: string;
  cancelledOrderCount: number; cancelledOrderValue: number;
  cancelledBillCount: number; cancelledBillValue: number;
  refundCount: number; refundAmount: number;
  cancelledOrders: { orderUuid: string; orderNumber: string; createdAt: string; cancelledAt: string;
                     cancelledBy: string | null; reason: string; lineCount: number; orderValue: number }[];
  cancelledBills: { billUuid: string; billNumber: string; orderNumber: string; billDate: string;
                    cancelledAt: string; cancelledBy: string | null; reason: string; billValue: number }[];
  refunds: { paymentUuid: string; orderUuid: string; orderNumber: string; method: string; amount: number;
             reason: string | null; refundedAt: string; refundedBy: string | null }[];
}
```

Newest first. `cancelledBy` / `refundedBy` are user uuids. Show them against the shop's staff list
to see who is cancelling a lot.

### 2.6 Hourly: `GET /reports/hourly?from&to`

Busy hours: completed orders per hour of the day, summed over the period. Always 24 rows (`hour` 0–23).

```ts
export interface HourlySalesReport {
  from: string; to: string;
  hours: { hour: number; orders: number; netSales: number; averageOrderValue: number }[];
}
```

### 2.7 Staff: `GET /reports/staff?from&to`

```ts
export interface StaffSalesRow {
  userUuid: string | null;    // kubee-auth user uuid: show the name from the shop's user list
  completedOrders: number;    // orders they created that were completed in the period
  netSales: number;
  discountAmount: number;     // discounts given on those orders
  paymentCount: number; paymentAmount: number;   // money they took
  refundCount: number; refundAmount: number;     // refunds they gave
  cancelledOrders: number; cancelledBills: number;
}
export interface StaffSalesReport { from: string; to: string; staff: StaffSalesRow[]; }
```

### 2.8a Channels: `GET /reports/channels?from&to`

Counter vs Zomato vs Swiggy. Completed orders per order `source` (by completion time) and orders cancelled in
the period. Only sources with activity are listed, biggest first; `total` has `source: null` and `shareOfSales: 100`.

```ts
export interface ChannelSalesRow {
  source: 'POS' | 'ZOMATO' | 'SWIGGY' | 'OTHER' | null;
  completedOrders: number;
  grossSales: number; discountAmount: number; taxAmount: number; netSales: number;
  averageOrderValue: number;
  shareOfSales: number;       // % of net sales
  cancelledOrders: number;
}
export interface ChannelSalesReport { from: string; to: string; channels: ChannelSalesRow[]; total: ChannelSalesRow; }
```

### 2.8 Categories: `GET /reports/categories?from&to`

Item-wise sales rolled up to each item's **current** category. `categoryUuid: null` = items without a category
(`categoryName: "Uncategorised"`). Same amounts and `shareOfSales` as the item report.

```ts
export interface CategorySalesRow {
  categoryUuid: string | null; categoryName: string; parentCategoryName: string | null;
  quantity: number; orderCount: number;
  grossAmount: number; discountAmount: number; taxableAmount: number; taxAmount: number; netAmount: number;
  shareOfSales: number;
}
export interface CategorySalesReport { from: string; to: string; totalNetAmount: number; categories: CategorySalesRow[]; }
```

### 2.9 Shifts (cash drawer): `GET /reports/shifts?from&to`

Shifts **opened** in the period, newest first. Open shifts show live figures and no count. How shifts work:
[`shifts-api.md`](shifts-api.md).

```ts
export interface ShiftReportRow {
  shiftUuid: string; status: 'OPEN' | 'CLOSED';
  openedAt: string; openedBy: string | null; closedAt: string | null; closedBy: string | null;
  openingCash: number; cashSales: number; cashIn: number; cashOut: number;
  expectedCash: number; countedCash: number | null;
  cashDifference: number | null;   // counted − expected; negative = short
  closingNotes: string | null;
}
export interface ShiftReport {
  from: string; to: string; shiftCount: number;
  totalShort: number;    // sum of shortages (positive number)
  totalExcess: number;
  shifts: ShiftReportRow[];
}
```

---

## 3. Downloads

### CSV / Excel: `GET /reports/{report}/export?format=csv|xlsx&from&to`

`{report}` is one of `sales-summary`, `payment-modes`, `channels`, `items`, `categories`, `hourly`, `staff`, `gst`,
`cancellations`, `shifts`. Same filters as the report (`items` also takes `categoryUuid` and `sort`).
`format` defaults to `csv`.

- The response is the file itself (not the `{code, message, data}` envelope), with
  `Content-Disposition: attachment; filename="items_2026-10-01_2026-10-31.xlsx"`.
- **Excel**: one sheet per table (e.g. GST has *GST totals*, *By rate*, *B2B invoices*, *B2C by state and rate*,
  *HSN summary*). Amounts are real numbers, so they can be summed in Excel.
- **CSV**: UTF-8 with BOM (opens correctly in Excel). A report with several tables writes them one after
  another, each starting with its title line and separated by an empty line.
- Dates are `DD-MM-YYYY` / `DD-MM-YYYY HH:mm`. Text typed by users (names, reasons) that starts with `=`, `+`,
  `-` or `@` gets a leading `'` so Excel never runs it as a formula.

Download in Angular (the token must be sent, so use `HttpClient`, not a plain link):

```ts
this.http.get(`/api/v1/reports/${report}/export`, {
  params: { format, from, to }, responseType: 'blob', observe: 'response',
}).subscribe(res => {
  const name = /filename="([^"]+)"/.exec(res.headers.get('Content-Disposition') ?? '')?.[1] ?? 'report';
  const url = URL.createObjectURL(res.body!);
  Object.assign(document.createElement('a'), { href: url, download: name }).click();
  URL.revokeObjectURL(url);
});
```

The backend exposes `Content-Disposition` through CORS, so the file name can be read from other origins too.

### GSTR-1 JSON: `GET /reports/gstr1?month=yyyy-MM`

The month's GSTR-1 as the JSON the GST portal's **offline tool** imports. File name
`GSTR1_<shop GSTIN>_<MMyyyy>.json`. Built from **issued** bills dated in that month.

| Section | Contents |
|---|---|
| `b2b` | Invoices to GST-registered buyers, grouped by buyer GSTIN, one item per tax rate |
| `b2cl` | B2C invoices to **another state** above ₹1,00,000, invoice-wise by place of supply |
| `b2cs` | All other B2C sales, by place of supply and rate (`INTRA` / `INTER`) |
| `nil` | 0% lines, reported as **nil-rated** |
| `hsn` | HSN summary split into `hsn_b2b` and `hsn_b2c`. Units are mapped to GST UQC codes (PLATE/CUP/GLASS → NOS, unknown → OTH); services (SAC 99…) use `NA` |
| `doc_issue` | Bill numbers used per series (first, last, total, cancelled) |

- The shop must have a **GSTIN** in settings, otherwise `422`.
- **Check the file in the GST offline tool before filing.** The portal changes the schema from time to time,
  and the CA should confirm two things: whether 0% items are nil-rated, exempt or non-GST, and the UQC for each unit.

---

## 4. Errors

| Status | `message` |
|---|---|
| `422` | `'from' must be on or before 'to'` |
| `422` | `A report can cover at most 366 days` |
| `400` | `Malformed request: Method parameter 'from' …` (bad date) / `'sort' …` (not `AMOUNT`/`QUANTITY`) / `'month' …` |
| `403` | `Only a manager or the owner can do this` |
| `404` | `No report called … Use one of […]` (export) |
| `422` | `Format must be csv or xlsx` |
| `422` | `Add the shop's GSTIN in settings before creating GSTR-1` |

---

## 5. Not built yet

- PDF versions of reports, and scheduled e-mail of the day-end summary.
- GSTR-3B figures, and credit/debit notes in GSTR-1 (credit notes aren't built yet).
