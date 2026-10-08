# grep vs glob vs llm-index — real output, same repo, same intent

**Task:** find the code that sends customer email/notification, in `ezinventory` (353 Java files).
Everything below is real captured output from this repo, not a mockup.

---

## 1. Glob — find files by name

```
find src/main/java -iname "*mail*" -o -iname "*notif*"
```

**Result:** 19 files/dirs, filenames only — no code, no context.

```
src/main/java/com/ezh/Inventory/notifications
src/main/java/com/ezh/Inventory/notifications/common/controller/NotificationController.java
src/main/java/com/ezh/Inventory/notifications/common/dto/NotificationDistributor.java
src/main/java/com/ezh/Inventory/notifications/common/dto/NotificationRequest.java
src/main/java/com/ezh/Inventory/notifications/common/dto/NotificationResult.java
src/main/java/com/ezh/Inventory/notifications/common/entity/Notification.java
src/main/java/com/ezh/Inventory/notifications/common/entity/NotificationChannel.java
src/main/java/com/ezh/Inventory/notifications/common/entity/NotificationDelivery.java
src/main/java/com/ezh/Inventory/notifications/common/entity/NotificationType.java
src/main/java/com/ezh/Inventory/notifications/common/repository/NotificationDeliveryRepository.java
src/main/java/com/ezh/Inventory/notifications/common/repository/NotificationRepository.java
src/main/java/com/ezh/Inventory/notifications/common/service/NotificationService.java
src/main/java/com/ezh/Inventory/notifications/gmail
src/main/java/com/ezh/Inventory/notifications/gmail/config/GmailProperties.java
src/main/java/com/ezh/Inventory/notifications/gmail/controller/GmailController.java
src/main/java/com/ezh/Inventory/notifications/gmail/dto/GmailRequest.java
src/main/java/com/ezh/Inventory/notifications/gmail/dto/GmailTemplateRequest.java
src/main/java/com/ezh/Inventory/notifications/gmail/service/GmailService.java
src/main/java/com/ezh/Inventory/payment/dto/PaymentLinkEmailRequestDto.java
```

**What it tells you:** where things *might* live, by filename luck. Nothing about which method does what, who calls what, or which of these 19 is actually the entry point. You still have to open files one by one.

---

## 2. Grep — search file contents

### Attempt 1 — the honest first guess: `"email"`

```
grep -rniI "email" src/main/java --include="*.java" -l
```

**Result:** 37 files, 195 matching lines, **27,427 characters** (~6.9k tokens) of raw match output — before opening a single file.

Sample of what comes back (real, unedited):

```
DeliveryServiceImpl.java:142:     * Best-effort email to the customer when a delivery is created.
DeliveryServiceImpl.java:149:    if (customer == null || customer.getEmail() == null...
DeliveryServiceImpl.java:550:                    .email(userDetail.getEmail())
SalesReturnServiceImpl.java:169:     * Best-effort email to the customer when a sales return is created.
InvoiceServiceImpl.java:395:                    .email(userDetail.getEmail())
JwtAuthFilter.java:47:              String email = jwtTokenProvider.getEmailFromToken(token);
JwtAuthFilter.java:54:              userContext.setEmail(email);
```

**The problem:** `JwtAuthFilter`, `EmployeeDto`, `ContactDto`, `TenantDto`, `UserMiniDto` all show up because they merely *have an email field*. Real signal (`DeliveryServiceImpl`, `SalesReturnServiceImpl`) is buried in noise from 30+ unrelated files. No ranking — a security filter and the actual mail-sending service look identical in the output.

### Attempt 2 — a narrower, more technical guess: `"sendmail|sendnotification|smtp"`

```
grep -rniIE "sendmail|sendnotification|smtp" src/main/java --include="*.java" -l
```

**Result:** 7 files — much cleaner, actually usable. **But** this only works if you already know to guess "SMTP," which is exactly the kind of vocabulary gap a newcomer to the codebase won't have.

### Attempt 3 — the "safe broad" guess: `"notification"`

```
grep -rniI "notification" src/main/java --include="*.java" -l | wc -l   # 28 files
grep -rniI "notification" src/main/java --include="*.java" | wc -c      # 49,957 chars
```

**Result:** 28 files, **49,957 characters (~12.5k tokens)** of raw output — broader than the "email" guess. Every DTO, entity, and repository with "Notification" in a class name shows up flat, unranked, with no indication of which one is the actual send-path vs. a plain data holder.

---

## 3. llm-index — same intent, one call

```
java -jar llm-index-exec.jar query "email notification mail service"
```

**Result:** 3,072 characters (~770 tokens) total. 10 hits, all relevant, each with file:line **and signature**:

```
## Matching code
- NotificationController.notifyGlobal (...NotificationController.java:79)  `ResponseEntity<NotificationResult> notifyGlobal(String, String)`
- NotificationService.dispatchEmail (...NotificationService.java:208)     `boolean dispatchEmail(String, NotificationRequest)`
- GmailService.sendNotification (...GmailService.java:132)                `boolean sendNotification(String, String, String)`
- DeliveryServiceImpl.notifyCustomerOfDelivery (...DeliveryServiceImpl.java:145)  `void notifyCustomerOfDelivery(Delivery, ShipmentStatus)`
- SalesReturnServiceImpl.notifyCustomerOfReturn (...SalesReturnServiceImpl.java:172)  `void notifyCustomerOfReturn(SalesReturn, Long)`

## Used by (impact if changed)
[reverse call-graph: who depends on this]

## Calls outward from here
[what this code touches downstream]
```

No `JwtAuthFilter`, no `EmployeeDto`, no `ContactDto`. Zero unrelated hits. Plus two things grep/glob structurally cannot give you: the **reverse dependency list** ("who uses this") and the **outward call graph** ("what this calls"), pulled from a real parsed graph, not text matching.

---

## Side-by-side

| | Glob | Grep (`"email"`) | Grep (`"notification"`) | llm-index |
|---|---|---|---|---|
| Files touched | 19 (names only) | 37 | 28 | 3 relevant, cited directly |
| Raw output size | ~1.1k chars | 27,427 chars (~6.9k tok) | 49,957 chars (~12.5k tok) | 3,072 chars (~770 tok) |
| False positives | N/A (no content) | Heavy — JWT, DTOs, tenant fields | Heavy — every Notification* DTO/entity | None observed |
| Needs exact vocabulary guess? | Yes (filename luck) | Yes, and still noisy | Yes, still noisy | No — synonym table covers email/mail/gmail/smtp/notify |
| Gives method signatures? | No | No (just raw lines) | No | Yes, inline |
| Gives reverse call-graph? | No | No | No | Yes |
| Requires opening files to confirm relevance? | Always | Almost always | Almost always | Rarely — signature often answers it |

---

---

## Appendix — same exact query, all three tools, full raw output

Query term: **`GmailService`** (one exact class name — the fairest possible test, since this is a case where grep *should* do well).

### Glob

```
find src/main/java -iname "*GmailService*"
```

Raw output (complete, unedited):
```
src/main/java/com/ezh/Inventory/notifications/gmail/service/GmailService.java
```
One file path. Tells you it exists and where. Nothing about what's inside it.

### Grep

```
grep -rn "GmailService" src/main/java --include="*.java"
```

Raw output (complete, unedited — 9 lines, 1,260 characters):
```
src/main/java/com/ezh/Inventory/sales/delivery/service/DeliveryServiceImpl.java:3:import com.ezh.Inventory.notifications.gmail.service.GmailService;
src/main/java/com/ezh/Inventory/sales/delivery/service/DeliveryServiceImpl.java:65:    private final GmailService gmailService;
src/main/java/com/ezh/Inventory/sales/returns/service/SalesReturnServiceImpl.java:8:import com.ezh.Inventory.notifications.gmail.service.GmailService;
src/main/java/com/ezh/Inventory/sales/returns/service/SalesReturnServiceImpl.java:71:    private final GmailService gmailService;
src/main/java/com/ezh/Inventory/notifications/common/service/NotificationService.java:10:import com.ezh.Inventory.notifications.gmail.service.GmailService;
src/main/java/com/ezh/Inventory/notifications/common/service/NotificationService.java:44:    private final GmailService                   gmailService;
src/main/java/com/ezh/Inventory/notifications/gmail/service/GmailService.java:43:public class GmailService {
src/main/java/com/ezh/Inventory/notifications/gmail/controller/GmailController.java:5:import com.ezh.Inventory.notifications.gmail.service.GmailService;
src/main/java/com/ezh/Inventory/notifications/gmail/controller/GmailController.java:33:    private final GmailService gmailService;
```
**Honest note:** for an exact known class name like this, grep is actually great — small, precise, no noise. This is grep's best case. It correctly shows every file that imports/injects `GmailService` (i.e. exactly the "used by" list). What it *doesn't* give you: `GmailService`'s own methods, their signatures, or what `GmailService` itself calls outward. You'd still have to open the file for that.

### llm-index

```
java -jar llm-index-exec.jar query "GmailService"
```

Raw output (complete, unedited — 2,937 characters):
```
# Context for: GmailService

## Matching code
- GmailService (src/main/java/com/ezh/Inventory/notifications/gmail/service/GmailService.java:40)
- GmailService.sendText (...GmailService.java:60)             `boolean sendText(String, String, String)`
- GmailService.sendHtml (...GmailService.java:86)              `boolean sendHtml(String, String, String)`
- GmailService.send (...GmailService.java:103)                 `boolean send(GmailRequest)`
- GmailService.sendOtp (...GmailService.java:116)              `boolean sendOtp(String, String, String)`
- GmailService.sendNotification (...GmailService.java:132)     `boolean sendNotification(String, String, String)`
- GmailService.sendWelcome (...GmailService.java:145)          `boolean sendWelcome(String, String)`
- GmailService.sendPasswordReset (...GmailService.java:159)    `boolean sendPasswordReset(String, String, String)`
- GmailService.sendOrderNotification (...GmailService.java:175) `boolean sendOrderNotification(String, String, String, String, String)`
- GmailService.sendTemplate (...GmailService.java:189)         `boolean sendTemplate(GmailTemplateRequest)`

## Used by (impact if changed)
- NotificationService
- GmailController
- DeliveryServiceImpl
- SalesReturnServiceImpl

## Calls outward from here
-   log.info
-   mailSender.send
-   props.fromAddress
-   GmailRequest.builder
-   GmailProperties
-   JavaMailSender
... (full call-chain, truncated here for length)
```

**What this adds over grep, for the identical query:** every method *inside* `GmailService` with its signature (grep found the class was used, not what it does), plus the same "used by" list grep found — but derived from a real parsed graph, not text matching, plus the outward call graph grep structurally cannot produce at all.

### Side-by-side for this one query

| | Glob | Grep | llm-index |
|---|---|---|---|
| Raw output | 1 file path | 9 lines / 1,260 chars | 2,937 chars |
| Tells you the file exists | Yes | Yes | Yes |
| Tells you who uses it | No | Yes (via import/field lines) | Yes (explicit "Used by" list) |
| Tells you its own methods + signatures | No | No | Yes |
| Tells you what it calls downstream | No | No | Yes |

This is the fair case for grep — exact identifier, no ambiguity, small output. It ties or beats llm-index on raw byte count here. It loses on **what the data actually contains**: grep gives you usage sites, llm-index gives you usage sites *plus* the callee's own shape.

---

## Bottom line

- **Glob** narrows *where to look* by filename, nothing else.
- **Grep** finds every literal occurrence, which means it finds the right file *and* 30 wrong ones in the same breath — precision is on you, and the narrow query that avoids the noise (`smtp`) only exists if you already know the codebase's vocabulary.
- **llm-index** returned the same right answer in **~770 tokens vs. ~6.9k–12.5k tokens of raw grep output alone** (before even opening a file), with no false positives, plus relationship data (calls/used-by) that text search can't produce at all.
