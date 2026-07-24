# Apartment Management System — Project Plan

## Stack
- Java 25, Spring Boot 4.1.0
- Spring MVC + Thymeleaf (server-rendered)
- Spring Data JPA + PostgreSQL
- Spring Security (role-based, Session-based)
- Flyway for schema migrations (do NOT rely on `hibernate.ddl-auto=update` past prototyping)
- Bean Validation (`jakarta.validation`) on DTOs
- Testcontainers (Postgres) for integration tests

---

## 1. Module Structure

Single Spring Boot module, package-by-feature (not package-by-layer — scales better as this grows):

```
com.apartment
├── user/          (User, Role, auth)
├── room/          (Room, RoomStatus)
├── contract/       (Contract/Lease)
├── tenant/        (Tenant profile, ID docs)
├── finance/       (UtilityRate, MeterReading, Invoice, Payment, Ledger)
├── maintenance/    (Ticket, TicketImage, TicketStatusUpdate)
├── announcement/   (News, Rules)
├── common/         (BaseEntity, exceptions, file storage, audit)
└── config/         (SecurityConfig, JpaConfig, WebConfig)
```

Each feature package: `domain/`, `repository/`, `service/`, `web/` (controller), `dto/`.

**Anti-pattern to avoid:** fat `UserController` handling all 5 roles' views. Split by role-specific controllers (`LandlordController`, `TechnicianController`, `TenantController`) even if they share services — keeps Thymeleaf templates and authorization boundaries clean.

---

## 2. User & Auth System

### Roles
```
LANDLORD, MANAGER, ACCOUNTANT, TECHNICIAN, TENANT
```

Use a single `Role` enum, not a separate mutable `roles` table, unless you need dynamic roles later (you don't, per spec).

```java
public enum Role {
    LANDLORD, MANAGER, ACCOUNTANT, TECHNICIAN, TENANT
}

@Entity
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    private String passwordHash; // BCrypt

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;

    @OneToOne(mappedBy = "user")
    private Tenant tenantProfile; // null unless role == TENANT
}
```

### Permission matrix

| Action | Landlord | Manager | Accountant | Technician | Tenant |
|---|---|---|---|---|---|
| Create/update any user | ✅ | ✅ | ❌ | ❌ | ❌ |
| Create Technician user | ✅ | ✅ | ❌ | ❌ | ❌ |
| Manage rooms/contracts | ✅ | ✅ | ❌ | ❌ | ❌ |
| View/update financials, accept payment | ✅ | ✅ | ✅ | ❌ | ❌ |
| Update assigned ticket status | ✅ | ✅ | ❌ | ✅ (own only) | ❌ |
| View own rent/invoice/receipt | — | — | — | — | ✅ |
| Create/track own ticket | — | — | — | — | ✅ |
| Publish announcements | ❌ | ✅ | ❌ | ❌ | ❌ |

Implement with `@PreAuthorize("hasRole('MANAGER')")` at service layer (not just controller) — controller-only checks are bypassable if services get reused (e.g. scheduled jobs, other controllers).

Manager > Landlord in permission scope per your spec — model this explicitly in a comment/doc since it's slightly unusual (normally landlord=owner=highest). Confirm this is intentional before building it in.

**Auth approach:** stick with Spring Security session-based login (`formLogin`) since this is server-rendered Thymeleaf, not an SPA. Don't reach for JWT here — it adds complexity (refresh tokens, storage) with no benefit for a monolithic MVC app.

---

## 3. Room System

```java
public enum RoomStatus { VACANT, OCCUPIED, MAINTENANCE }

@Entity
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String roomNumber;
    private String floor;
    private BigDecimal baseRent;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;
}
```

**Status transitions should be driven, not hand-set:**
- `VACANT → OCCUPIED`: on active contract creation
- `OCCUPIED → VACANT`: on contract end/termination
- `→ MAINTENANCE`: manual, blocks new contract creation

Enforce via a `RoomService.transition(room, newStatus)` with a validated state-transition map — don't let controllers `room.setStatus(x)` directly. Prevents e.g. marking an occupied room vacant while a contract is still active.

---

## 4. Contract / Lease System

```java
@Entity
public class Contract {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Room room;

    @ManyToOne(optional = false)
    private Tenant tenant;

    private BigDecimal monthlyRent;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ContractStatus status; // ACTIVE, EXPIRED, TERMINATED
}
```

- Use `BigDecimal` for all money fields — never `double`/`float`.
- Add a DB constraint (or `@AssertTrue` validator) ensuring no overlapping active contracts per room.
- Renewal = new `Contract` row referencing same room/tenant, not mutating dates on the old one — preserves history for invoicing/audit.

---

## 5. Tenant System

```java
@Entity
public class Tenant {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    private User user; // login account, nullable if tenant has no portal access

    private String fullName;
    private String phone;
    private String email;
    private String idCardNumber;
    private String idCardImagePath; // stored file reference, not BLOB in DB
}
```

- Store ID card images on disk/object storage (local volume or S3-compatible, e.g. MinIO — you've used this before), keep only the path/key in Postgres. Don't store images as `bytea`.
- ID card number and image are sensitive — encrypt at rest or at minimum restrict read access to LANDLORD/MANAGER/ACCOUNTANT roles at the service layer.

---

## 6. Finance System (core complexity — design this first)

### 6.1 Utility rates (monthly, variable)

```java
@Entity
public class UtilityRate {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private YearMonth period;       // e.g. 2026-01
    private BigDecimal electricityUnitPrice;
    private BigDecimal waterUnitPrice;
}
```

One row per month, entered by Accountant/Manager before invoice generation runs.

### 6.2 Meter readings

```java
@Entity
public class MeterReading {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne private Room room;
    private YearMonth period;
    private BigDecimal electricityReading;
    private BigDecimal waterReading;
}
```

Usage = `currentReading - previousReading` (lookup prior period's row for the same room). Guard against missing prior reading (first month) and negative deltas (meter reset/rollover) — flag for manual review rather than silently computing garbage.

### 6.3 Invoice & Invoice Lines

```java
@Entity
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne private Contract contract;
    private YearMonth period;
    private BigDecimal rentAmount;
    private BigDecimal electricityAmount;
    private BigDecimal waterAmount;
    private BigDecimal totalAmount;      // sum, computed at generation time
    private BigDecimal balanceRemaining; // decremented as payments applied

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status; // UNPAID, PARTIALLY_PAID, PAID

    private LocalDate dueDate;
}
```

Generate one `Invoice` per contract per month via a scheduled job (`@Scheduled` cron, 1st of month) — don't generate on-demand at view time, invoices must be immutable historical records once issued.

### 6.4 Payments & Installments (the tricky part)

Model this as a **ledger**, not a single "amount paid" field on Invoice. A payment can be partial, and future payments must apply to the **oldest unpaid invoice first** (FIFO), per your Bob example.

```java
@Entity
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne private Tenant tenant;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    @ManyToOne private User receivedBy; // accountant who processed it

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<PaymentAllocation> allocations = new ArrayList<>();
}

@Entity
public class PaymentAllocation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne private Payment payment;
    @ManyToOne private Invoice invoice;
    private BigDecimal amountApplied;
}
```

**Allocation algorithm** — this is the core business rule, put it in `PaymentService`, unit-test it directly with the Bob scenario:

```java
@Transactional
public Payment recordPayment(Tenant tenant, BigDecimal amount, User receivedBy) {
    Payment payment = new Payment(tenant, amount, LocalDateTime.now(), receivedBy);
    BigDecimal remaining = amount;

    List<Invoice> unpaidInvoices = invoiceRepository
        .findByContractTenantAndStatusInOrderByPeriodAsc(
            tenant, List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID));

    for (Invoice invoice : unpaidInvoices) {
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

        BigDecimal owed = invoice.getBalanceRemaining();
        BigDecimal applied = remaining.min(owed);

        payment.getAllocations().add(new PaymentAllocation(payment, invoice, applied));
        invoice.setBalanceRemaining(owed.subtract(applied));
        invoice.setStatus(invoice.getBalanceRemaining().compareTo(BigDecimal.ZERO) == 0
            ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);

        remaining = remaining.subtract(applied);
    }

    // remaining > 0 here = tenant overpaid beyond all outstanding invoices.
    // Decide: credit balance for next month, or reject. Don't silently drop it.

    return paymentRepository.save(payment);
}
```

Verify against your example:
- Jan invoice 3000, pay 2000 → Jan balance 1000, status PARTIALLY_PAID.
- Feb invoice 3000 generated → tenant now has [Jan:1000, Feb:3000] outstanding.
- Pay 4000 → applies 1000 to Jan (PAID), then 3000 to Feb (PAID), remaining 0. ✅ matches spec.

**Edge cases to handle explicitly:**
- Overpayment beyond all invoices (credit vs. reject — pick one, document it)
- Payment during month the invoice hasn't been generated yet
- Concurrent payments for the same tenant (`@Transactional` + row-level lock via `@Lock(PESSIMISTIC_WRITE)` on invoices, or a `SERIALIZABLE` isolation retry — two accountants recording payment simultaneously must not double-allocate)

### 6.5 Receipts
Generate a `Receipt` (PDF, e.g. via OpenPDF/Flying Saucer + Thymeleaf template) per `Payment`, listing its `PaymentAllocation`s — this is what makes a partial/split payment auditable to the tenant.

### 6.6 Financial dashboard
Aggregate queries (native SQL or JPQL projections, not entity loading + Java-side summation):
- Total outstanding balance (sum of `balanceRemaining` where status != PAID)
- Monthly revenue collected vs. billed
- Overdue invoices (dueDate < today AND status != PAID)

Use `@Query` with DTO projections (`interface` or `record` projections) — don't fetch full entity graphs for dashboard aggregates.

---

## 7. Room Maintenance / Ticketing System

```java
public enum TicketStatus { PENDING, IN_PROGRESS, COMPLETED }

@Entity
public class MaintenanceTicket {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne private Room room;
    @ManyToOne private Tenant reportedBy;
    @ManyToOne private User assignedTechnician; // nullable until triaged

    private String description;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.PENDING;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<TicketImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orderBy = "createdAt ASC")
    private List<TicketStatusUpdate> history = new ArrayList<>();
}

@Entity
public class TicketImage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne private MaintenanceTicket ticket;
    private String imagePath;
    private TicketImageType type; // REPORTED, PROOF_OF_FIX
}

@Entity
public class TicketStatusUpdate {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne private MaintenanceTicket ticket;
    @Enumerated(EnumType.STRING) private TicketStatus status;
    @ManyToOne private User updatedBy;
    private LocalDateTime createdAt;
    private String note;
}
```

- Enforce `assignedTechnician == currentUser` at service layer before allowing status updates — a technician updating someone else's ticket is a broken-access-control bug waiting to happen.
- COMPLETED transition should require at least one `PROOF_OF_FIX` image — validate in service, not just UI.
- Tenant "track ticket" view = read-only projection filtered to `reportedBy == currentTenant`.

---

## 8. Info & Announcement System

```java
@Entity
public class Announcement {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String title;
    @Lob private String content; // or markdown, rendered via Thymeleaf
    @Enumerated(EnumType.STRING) private AnnouncementType type; // NEWS, RULE
    @ManyToOne private User publishedBy;
    private LocalDateTime publishedAt;
    private boolean pinned = false;
}
```

Manager-only publish, per spec — enforce with `@PreAuthorize("hasRole('MANAGER')")`.

---

## 9. Lease Renewal Warning

- Scheduled job or on-demand check in tenant dashboard: `contract.endDate.isBefore(today.plusDays(30))`
- Don't hardcode 30 — make it a config property (`app.lease.renewal-warning-days=30`)
- Surface as a banner on tenant home view; optionally also email/notify Landlord/Manager for renewal follow-up

---

## 10. Suggested Build Order

1. **Foundation**: User/auth/roles, Spring Security config, base entity/audit fields (`createdAt`/`updatedAt` via `@EntityListeners(AuditingEntityListener.class)`)
2. **Room + Tenant + Contract** (core relational backbone, no business logic yet)
3. **Finance core**: UtilityRate, MeterReading, Invoice generation job
4. **Payment/PaymentAllocation** + installment logic — write unit tests against the Bob scenario before wiring UI
5. **Receipts/PDF generation**
6. **Maintenance ticketing** (independent of finance, can parallelize with step 3–5)
7. **Announcements**
8. **Dashboards** (financial + lease renewal warnings) — built last since they aggregate everything above
9. Thymeleaf views per role, styled last

---

## 11. Things to decide before coding (flag to stakeholder / yourself)

- Manager having *more* permission than Landlord — confirm intentional (spec has it inverted from typical ownership hierarchy).
- Overpayment handling: credit forward or reject?
- File storage target: local disk vs MinIO/S3 (you've used MinIO before — reuse that pattern here for ID cards + ticket images + receipts).
- Multi-tenancy: is this single-building or does it need to support multiple properties/landlords sharing one deployment? Not stated in spec — affects whether `Room`/`Contract` need a `Property`/`Building` parent entity now vs. later migration pain.
