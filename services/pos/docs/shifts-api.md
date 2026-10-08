# Shifts (Cash Drawer) API: Frontend Guide (Angular)

Open the day with the cash in the drawer, record cash put in or taken out, and close by counting the cash.
The server works out how much cash there **should** be and the difference.

- Base path: **`/api/v1/shifts`**. Headers, envelope and errors: see [`catalog-api.md`](catalog-api.md) section 1.
- Any staff member can open, record cash and close. Shift history is a manager report:
  `GET /api/v1/reports/shifts` ([`reports-api.md`](reports-api.md)).

---

## 1. How it works

```
open (opening cash) ──► cash in / cash out (with reason) ──► close (counted cash)
```

- **One open shift per shop** at a time (one cash drawer). Opening another while one is open gives `409`.
- **Expected cash** = opening cash + **cash sales** + cash in − cash out, where cash sales = successful CASH
  payments − CASH refunds made between opening and closing.
- **Difference** = counted − expected. Negative = cash is short, positive = excess.
- A closed shift never changes.
- Billing works with or without an open shift (shifts are not enforced).

---

## 2. Models

```ts
export interface ShiftView {
  uuid: string;
  status: 'OPEN' | 'CLOSED';
  openedBy: string | null; openedAt: string; openingCash: number; openingNotes: string | null;
  closedBy: string | null; closedAt: string | null; closingNotes: string | null;
  cashSales: number;        // live while OPEN
  cashIn: number;
  cashOut: number;
  expectedCash: number;     // live while OPEN
  countedCash: number | null;      // set on close
  cashDifference: number | null;   // counted − expected
  completedOrders: number;  // orders completed during the shift
  netSales: number;
  payments: { method: string; paymentCount: number; paymentAmount: number;
              refundCount: number; refundAmount: number; netAmount: number }[];   // all methods, during the shift
  movements: { uuid: string; type: 'IN' | 'OUT'; amount: number; reason: string;
               createdBy: string | null; movedAt: string }[];
}
```

---

## 3. Endpoints

All return the `ShiftView`.

| Method | Path | Body | Notes |
|---|---|---|---|
| `POST` | `/api/v1/shifts` | `{ "openingCash": 2000, "notes": "morning" }` | `201`. `409` if a shift is already open |
| `GET` | `/api/v1/shifts/current` | | The open shift with live figures; `404` `No shift is open` |
| `GET` | `/api/v1/shifts/{uuid}` | | Any shift |
| `POST` | `/api/v1/shifts/{uuid}/cash-movements` | `{ "type": "OUT", "amount": 500, "reason": "Milk supplier" }` | `IN` = float / change added, `OUT` = cash paid out. `422` if the shift is closed |
| `POST` | `/api/v1/shifts/{uuid}/close` | `{ "countedCash": 6350, "notes": "50 short" }` | Stores expected cash and the difference |

## 4. Screen recipe

1. On login: `GET /shifts/current`. On `404`, ask "Opening cash in drawer?" → `POST /shifts`.
2. "Cash in / out" button → `POST /cash-movements` with a reason (required).
3. Day end: show `expectedCash` from `GET /shifts/current`, but **ask the cashier to count first**, then
   `POST /close` with `countedCash`. Show `cashDifference` (red when negative).

## 5. Not built yet

- Several cash drawers (counters) at once: payments don't record which counter took them yet.
- Forcing an open shift before billing (a shop setting).
- Denomination-wise counting (₹500 × n, ₹200 × n …).
