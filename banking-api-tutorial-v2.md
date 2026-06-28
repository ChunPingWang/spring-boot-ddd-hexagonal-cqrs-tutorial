# 銀行帳戶查詢 API — 架構規劃 Tutorial v2

> **技術棧**：Java 23 · Spring Boot 4 · DDD 戰術設計 · Hexagonal Architecture · SOLID · CQRS · TDD · BDD
> **業務範圍**：台幣/外幣活存交易紀錄查詢、轉帳優惠查詢與使用紀錄查詢
> **修訂說明**：v2 修正 Repository Interface 歸屬層次，確保 Domain Layer 純粹性；業務規則回歸 Aggregate，符合 DIP 與 Hexagonal Architecture 設計原則。

---

## 目錄

1. [業務需求說明](#1-業務需求說明)
2. [架構核心原則宣告](#2-架構核心原則宣告)
3. [系統架構概覽](#3-系統架構概覽)
4. [Hexagonal Architecture 分層設計](#4-hexagonal-architecture-分層設計)
5. [DDD 戰術設計](#5-ddd-戰術設計)
6. [CQRS 設計](#6-cqrs-設計)
7. [API 設計規範](#7-api-設計規範)
8. [TDD 設計規劃](#8-tdd-設計規劃)
9. [BDD 設計規劃](#9-bdd-設計規劃)
10. [專案結構](#10-專案結構)
11. [工項清單 Work Breakdown Structure](#11-工項清單-work-breakdown-structure)
12. [技術選型說明](#12-技術選型說明)
13. [非功能性需求考量](#13-非功能性需求考量)
14. [附錄：SOLID 原則對應表](#14-附錄solid-原則對應表)
15. [ADR：Repository Pattern 設計決策](#15-adrrepository-pattern-設計決策)

---

## 1. 業務需求說明

### 1.1 使用者情境

| 功能 | 說明 | 對應角色 |
|------|------|----------|
| 台幣活存交易紀錄查詢 | 客戶可依日期區間查詢台幣帳戶進出交易 | 已認證銀行客戶 |
| 外幣活存交易紀錄查詢 | 客戶可依幣別與日期區間查詢外幣帳戶交易 | 已認證銀行客戶 |
| 轉帳優惠內容查詢 | 查詢目前可用之轉帳優惠方案（例如免手續費次數） | 已認證銀行客戶 |
| 轉帳優惠使用紀錄查詢 | 查詢客戶已使用的轉帳優惠歷史紀錄 | 已認證銀行客戶 |

### 1.2 核心業務規則

- 客戶只能查詢自己名下的帳戶資料（**所有權驗證封裝於 Aggregate 內部**）
- 外幣交易紀錄需呈現原幣金額與台幣等值金額
- 查詢區間不得超過 13 個月（**由 Aggregate 的 Domain Method 強制執行**）
- 轉帳優惠有效期限與使用上限需即時反映
- 所有查詢操作需留存稽核日誌（Audit Log）

---

## 2. 架構核心原則宣告

在進入設計細節前，先明確宣告各層之間的**依賴方向規則**，作為所有設計決策的基準。

### 2.1 依賴方向（Dependency Rule）

```
Infrastructure Layer  →  Application Layer  →  Domain Layer
   (最外層)                  (協調層)             (最內層，純粹)

規則：箭頭方向 = 「依賴」方向
      內層永遠不知道外層的存在
      Domain Layer 不依賴任何人
```

### 2.2 各層對外部的感知邊界

| 層次 | 可依賴 | 絕對不可依賴 |
|------|--------|------------|
| **Domain Layer** | 自身的 Model / Value Object / Exception | Application、Infrastructure、Spring、JPA、任何 Framework |
| **Application Layer** | Domain Layer、自定義 Port Interfaces（in/out） | Infrastructure 實作類別（JPA Adapter、Controller）|
| **Infrastructure Layer** | Application Layer（Port Interface）、Domain Layer、Spring / JPA / Redis 等 Framework | 無限制 |

### 2.3 Repository Interface 的正確歸屬

```
❌ 錯誤觀念：Repository Interface 屬於 Domain Layer
✅ 正確觀念：Repository Interface 屬於 Application Layer（Output Port）

理由：
  Repository 的存在是為了讓 Application Layer 能取得 Domain Object。
  定義「需要什麼」的人是 Application Layer，
  因此 Interface 定義權在 Application Layer（port/out/）。
  Domain Layer 只定義 Model 本身，對資料如何取得一無所知。
```

---

## 3. 系統架構概覽

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                             │
│              Mobile App / Web App / Third-party                 │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTPS / JWT
┌───────────────────────────▼─────────────────────────────────────┐
│                    API Gateway / BFF                             │
│              Rate Limiting · Auth Verification                  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│              Banking Account Query Service                       │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │   Infrastructure — Driving Adapters (Inbound)            │  │
│  │         REST Controller (Spring MVC)                     │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                        │ calls Input Port (Interface)           │
│  ┌─────────────────────▼────────────────────────────────────┐  │
│  │   Application Layer — Query Handlers (CQRS Read Side)    │  │
│  │   - 協調流程：呼叫 Output Port 取得 Aggregate            │  │
│  │   - 委派業務規則執行至 Domain Model                       │  │
│  │   - 轉換結果為 Read Model（DTO）                          │  │
│  └──────────┬──────────────────────────┬─────────────────────┘  │
│             │ calls Output Port         │ uses Domain Objects    │
│             │ (Interface, defined here) │                        │
│  ┌──────────▼───────────┐   ┌──────────▼──────────────────┐    │
│  │  Infrastructure      │   │  Domain Layer（純粹）        │    │
│  │  Driven Adapters     │   │  - Aggregates               │    │
│  │  (implements Port)   │   │  - Value Objects            │    │
│  │  - JPA Adapter       │   │  - Domain Exceptions        │    │
│  │  - Cache Adapter     │   │  無任何外部依賴              │    │
│  │  - CoreBanking Adp.  │   └─────────────────────────────┘    │
│  └──────────────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   PostgreSQL /        Core Banking          Redis Cache
   Read DB             System (IBM)
```

---

## 4. Hexagonal Architecture 分層設計

### 4.1 Port & Adapter 對應關係

```
                    ┌──────────────────────────────────┐
                    │          Hexagon（六角形）         │
                    │                                   │
REST Client ──►  [Input Port]  ──►  Application  ──►  [Output Port]  ──►  DB
               (Interface,           Layer           (Interface,         JPA Adapter
                defined in          Handler          defined in          實作 Port)
                app/port/in/)                        app/port/out/)
                    │                                   │
             Driving Adapter                     Driven Adapter
             (Controller 實作呼叫)               (JPA / Cache 實作 Port)
                    │                                   │
                    └──────────────────────────────────┘

  依賴方向：Controller → Input Port ← Handler → Output Port ← JPA Adapter
  Domain Layer 位於六角形中心，不被任何 Port 依賴，也不依賴任何 Port
```

### 4.2 各層職責說明

#### 4.2.1 Domain Layer（純粹核心，無任何外部依賴）

```
domain/
├── model/
│   ├── account/        # Account Aggregate
│   ├── privilege/      # TransferPrivilege Aggregate
│   └── shared/         # 共用 Value Objects
└── exception/          # Domain Exceptions（純業務語意）
```

**設計約束**：
- `build.gradle.kts` 中 Domain module **不引入任何 Spring / JPA / 外部 Library**
- 不定義 Repository Interface（這是 Application Layer 的職責）
- 所有業務規則（所有權驗證、查詢區間限制）封裝於 Aggregate Method 內

#### 4.2.2 Application Layer（Use Cases，定義 Ports）

```
application/
├── port/
│   ├── in/             # Input Ports（Use Case Interfaces，由 Handler 實作）
│   └── out/            # Output Ports（Repository / External Interfaces，由 Adapter 實作）
└── query/
    ├── account/        # Query Objects + Handlers + Read Models
    └── privilege/      # Query Objects + Handlers + Read Models
```

**設計約束**：
- Output Port Interface **定義在此層**（`application/port/out/`），不在 Domain Layer
- Handler 只做流程協調：取得 Aggregate → 呼叫 Domain Method → 轉換 DTO
- Handler 不包含任何業務規則判斷（`if/else` 業務邏輯應在 Domain）

#### 4.2.3 Infrastructure Layer（Adapters，實作 Ports）

```
infrastructure/
├── adapter/
│   ├── in/rest/        # Driving Adapters（實作呼叫 Input Port）
│   └── out/
│       ├── persistence/ # Driven Adapters（實作 Output Port — AccountRepository 等）
│       ├── corebanking/ # Driven Adapters（實作 Output Port — ExchangeRatePort 等）
│       └── cache/       # Driven Adapters（實作 Output Port — PrivilegeCachePort 等）
└── config/
```

---

## 5. DDD 戰術設計

### 5.1 Bounded Context 劃分

```
┌──────────────────────────┐    ┌──────────────────────────┐
│    Account Context       │    │   Privilege Context      │
│                          │    │                          │
│  Account (Aggregate)     │    │  TransferPrivilege       │
│  Transaction (Entity)    │    │  (Aggregate)             │
│  Money (Value Object)    │    │  PrivilegeUsageRecord    │
│  DateRange (VO)          │    │  (Entity)                │
│  Currency (VO)           │    │  PrivilegeType (Enum)    │
└──────────────────────────┘    └──────────────────────────┘
              ▲                               ▲
              └──────────── Shared ──────────┘
                     CustomerId (Value Object)
                     AccountId  (Value Object)
                     Money      (Value Object)
                     DateRange  (Value Object)
```

### 5.2 Domain Model 設計

#### 5.2.1 Shared Value Objects（domain/model/shared/）

```java
// Money — 不可變，封裝金額與幣別的業務語意
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("金額最多 2 位小數");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不可為負數");
        }
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public static Money twd(BigDecimal amount) {
        return new Money(amount, Currency.TWD);
    }
}

// DateRange — 封裝日期區間業務語意與驗證
public record DateRange(LocalDate startDate, LocalDate endDate) {
    public DateRange {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate 不可晚於 endDate");
        }
    }

    public boolean exceedsMonths(int months) {
        return ChronoUnit.MONTHS.between(startDate, endDate) > months;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}

// CustomerId — 強型別，防止原始型別濫用（Primitive Obsession）
public record CustomerId(String value) {
    public CustomerId {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("CustomerId 不可為空");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}

// AccountId — 封裝帳號格式驗證
public record AccountId(String value) {
    public AccountId {
        Objects.requireNonNull(value);
        if (!value.matches("\\d{14}")) {
            throw new InvalidAccountIdFormatException("帳號格式不正確，需為 14 位數字");
        }
    }
}
```

#### 5.2.2 Account Aggregate（domain/model/account/）

```java
// Account — Aggregate Root
// 業務規則完全封裝於此，Application Layer 只協調，不判斷
public class Account {
    private final AccountId accountId;
    private final CustomerId ownerId;          // 帳戶持有人
    private final AccountType accountType;     // TWD / FX
    private final Currency currency;
    private AccountStatus status;              // ACTIVE / FROZEN / CLOSED

    // ── 業務規則 1：所有權驗證 ──────────────────────────────────────
    // Application Layer 取得 Account 後呼叫此方法，無需傳入任何外部依賴
    public void verifyOwnership(CustomerId requesterId) {
        if (!this.ownerId.equals(requesterId)) {
            throw new AccountNotOwnedByCustomerException(this.accountId, requesterId);
        }
    }

    // ── 業務規則 2：帳戶狀態驗證 ────────────────────────────────────
    public void ensureActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(this.accountId, this.status);
        }
    }

    // ── 業務規則 3：查詢區間限制（13 個月）─────────────────────────
    // 傳入的 transactions 是 Application Layer 透過 Output Port 取得後傳入
    // Account 只負責執行業務規則，不知道資料從哪裡來
    public TransactionHistory filterByDateRange(List<Transaction> transactions,
                                                 DateRange dateRange) {
        if (dateRange.exceedsMonths(13)) {
            throw new QueryRangeExceededException("查詢區間不可超過 13 個月");
        }
        if (this.accountType == AccountType.TWD && this.currency != Currency.TWD) {
            throw new AccountCurrencyMismatchException(this.accountId);
        }
        var filtered = transactions.stream()
            .filter(t -> dateRange.contains(t.transactionDate().toLocalDate()))
            .toList();
        return new TransactionHistory(this.accountId, filtered, dateRange);
    }

    // ── Query Methods（給 Application Layer 讀取狀態用）────────────
    public boolean isOwnedBy(CustomerId customerId) {
        return this.ownerId.equals(customerId);
    }

    public AccountId getAccountId()     { return accountId; }
    public AccountType getAccountType() { return accountType; }
    public Currency getCurrency()       { return currency; }
    public AccountStatus getStatus()    { return status; }
}

// Transaction — Entity（屬於 Account Aggregate 邊界內）
public class Transaction {
    private final TransactionId transactionId;
    private final TransactionType type;        // CREDIT / DEBIT
    private final Money amount;                // 原幣金額
    private final Money twdEquivalent;         // 台幣等值（外幣帳戶才有）
    private final LocalDateTime transactionDate;
    private final String description;
    private final TransactionChannel channel;

    // Query Methods
    public TransactionId getTransactionId()   { return transactionId; }
    public TransactionType getType()          { return type; }
    public Money getAmount()                  { return amount; }
    public Money getTwdEquivalent()           { return twdEquivalent; }
    public LocalDateTime transactionDate()    { return transactionDate; }
    public String getDescription()            { return description; }
    public TransactionChannel getChannel()    { return channel; }
}

// TransactionHistory — Value Object（查詢結果封裝）
public record TransactionHistory(
    AccountId accountId,
    List<Transaction> transactions,
    DateRange queriedRange
) {
    public TransactionHistory {
        Objects.requireNonNull(accountId);
        transactions = List.copyOf(transactions);   // 防禦性複製，確保不可變
        Objects.requireNonNull(queriedRange);
    }

    public int count() {
        return transactions.size();
    }
}
```

#### 5.2.3 TransferPrivilege Aggregate（domain/model/privilege/）

```java
// TransferPrivilege — Aggregate Root
public class TransferPrivilege {
    private final PrivilegeId privilegeId;
    private final CustomerId ownerId;
    private final PrivilegeType type;
    private final int totalQuota;
    private final int usedQuota;
    private final DateRange validPeriod;
    private final List<PrivilegeUsageRecord> usageRecords;

    // ── 業務規則 1：優惠是否有效 ────────────────────────────────────
    public boolean isValid() {
        return isWithinValidPeriod() && hasRemainingQuota();
    }

    private boolean isWithinValidPeriod() {
        return validPeriod.contains(LocalDate.now());
    }

    private boolean hasRemainingQuota() {
        return getRemainingQuota() > 0;
    }

    // ── 業務規則 2：剩餘次數計算 ────────────────────────────────────
    public int getRemainingQuota() {
        return totalQuota - usedQuota;
    }

    // ── 業務規則 3：使用紀錄過濾（由 Application Layer 傳入完整紀錄後執行）
    public PrivilegeUsageHistory filterUsageHistory(DateRange dateRange) {
        var filtered = usageRecords.stream()
            .filter(r -> dateRange.contains(r.usedDate()))
            .toList();
        return new PrivilegeUsageHistory(this.privilegeId, filtered, dateRange);
    }

    // ── 業務規則 4：所有權驗證 ──────────────────────────────────────
    public void verifyOwnership(CustomerId requesterId) {
        if (!this.ownerId.equals(requesterId)) {
            throw new PrivilegeNotOwnedByCustomerException(this.privilegeId, requesterId);
        }
    }

    // Query Methods
    public PrivilegeId getPrivilegeId()  { return privilegeId; }
    public int getTotalQuota()           { return totalQuota; }
    public int getUsedQuota()            { return usedQuota; }
    public DateRange getValidPeriod()    { return validPeriod; }
    public PrivilegeType getType()       { return type; }
}
```

#### 5.2.4 Domain Exceptions（domain/exception/）

```java
// 所有 Exception 使用業務語意命名，不含任何技術細節
public class AccountNotOwnedByCustomerException extends RuntimeException {
    public AccountNotOwnedByCustomerException(AccountId accountId, CustomerId customerId) {
        super("帳戶 [%s] 不屬於客戶 [%s]".formatted(accountId.value(), customerId.value()));
    }
}

public class QueryRangeExceededException extends RuntimeException {
    public QueryRangeExceededException(String message) {
        super(message);
    }
}

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(AccountId accountId, AccountStatus status) {
        super("帳戶 [%s] 狀態為 [%s]，無法查詢".formatted(accountId.value(), status));
    }
}
```

---

## 6. CQRS 設計

### 6.1 CQRS Read Side 架構

```
┌──────────────────────────────────────────────────────────────────┐
│                       CQRS Read Side                             │
│                                                                  │
│  Query Object        Input Port          Query Handler           │
│  (Immutable DTO) ──► (Interface) ──────► (Application Layer)    │
│                                                ↓ calls           │
│                                          Output Port             │
│                                          (Interface)             │
│                                                ↓ implemented by  │
│                                          Driven Adapter          │
│                                          (Infrastructure)        │
│                                                ↓                 │
│                                          Read DB / Cache         │
└──────────────────────────────────────────────────────────────────┘

Write Side：本 Tutorial 聚焦查詢功能，不涵蓋 Command Side。
            未來擴充轉帳指令時，需補充 Command / CommandHandler / Event 設計。
```

### 6.2 Output Port Interfaces（application/port/out/）

```java
// ── 帳戶 Output Ports ────────────────────────────────────────────

public interface LoadAccountPort {
    // 取得帳戶基本資料（不含交易）
    Optional<Account> findByAccountId(AccountId accountId);
    List<Account> findAllByCustomerId(CustomerId customerId);
}

public interface LoadTransactionPort {
    // 取得原始交易資料，由 Account Aggregate 執行過濾與業務驗證
    List<Transaction> findByAccountId(AccountId accountId, DateRange dateRange);
}

// ── 優惠 Output Ports ────────────────────────────────────────────

public interface LoadPrivilegePort {
    List<TransferPrivilege> findByCustomerId(CustomerId customerId);
    Optional<TransferPrivilege> findByPrivilegeId(PrivilegeId privilegeId);
}

public interface LoadPrivilegeUsagePort {
    List<PrivilegeUsageRecord> findByPrivilegeId(PrivilegeId privilegeId, DateRange dateRange);
}
```

> **設計說明**：Port 以「動詞 + 對象」命名（Load/Save/Send），不使用 `Repository` 字眼，
> 明確表達 Application Layer 的意圖，而非暗示持久化技術。

### 6.3 Input Port Interfaces（application/port/in/）

```java
// Input Port — 定義 Use Case 契約
public interface GetTwdTransactionHistoryUseCase {
    TwdTransactionHistoryResult execute(GetTwdTransactionHistoryQuery query);
}

public interface GetFxTransactionHistoryUseCase {
    FxTransactionHistoryResult execute(GetFxTransactionHistoryQuery query);
}

public interface GetTransferPrivilegeUseCase {
    TransferPrivilegeResult execute(GetTransferPrivilegeQuery query);
}

public interface GetPrivilegeUsageHistoryUseCase {
    PrivilegeUsageHistoryResult execute(GetPrivilegeUsageHistoryQuery query);
}
```

### 6.4 Query Objects（application/query/）

```java
// 所有 Query Objects 使用 record（不可變）
public record GetTwdTransactionHistoryQuery(
    CustomerId customerId,
    AccountId accountId,
    DateRange dateRange,
    int page,
    int size
) {}

public record GetFxTransactionHistoryQuery(
    CustomerId customerId,
    AccountId accountId,
    Currency currency,
    DateRange dateRange,
    int page,
    int size
) {}

public record GetTransferPrivilegeQuery(
    CustomerId customerId
) {}

public record GetPrivilegeUsageHistoryQuery(
    CustomerId customerId,
    PrivilegeId privilegeId,
    DateRange dateRange,
    int page,
    int size
) {}
```

### 6.5 Query Handlers（application/query/handler/）

```java
// ── GetTwdTransactionHistoryHandler ──────────────────────────────
@Component
public class GetTwdTransactionHistoryHandler implements GetTwdTransactionHistoryUseCase {

    private final LoadAccountPort loadAccountPort;       // Output Port（DI 注入）
    private final LoadTransactionPort loadTransactionPort; // Output Port（DI 注入）

    public GetTwdTransactionHistoryHandler(LoadAccountPort loadAccountPort,
                                           LoadTransactionPort loadTransactionPort) {
        this.loadAccountPort = loadAccountPort;
        this.loadTransactionPort = loadTransactionPort;
    }

    @Override
    public TwdTransactionHistoryResult execute(GetTwdTransactionHistoryQuery query) {

        // Step 1：透過 Output Port 取得 Aggregate（Application Layer 職責）
        Account account = loadAccountPort.findByAccountId(query.accountId())
            .orElseThrow(() -> new AccountNotFoundException(query.accountId()));

        // Step 2：委派業務規則至 Domain Model（所有權 + 帳戶狀態）
        account.verifyOwnership(query.customerId());   // Domain 執行業務規則
        account.ensureActive();                         // Domain 執行業務規則

        // Step 3：透過 Output Port 取得原始交易資料
        List<Transaction> rawTransactions =
            loadTransactionPort.findByAccountId(query.accountId(), query.dateRange());

        // Step 4：委派業務規則至 Domain Model（查詢區間限制 + 過濾）
        TransactionHistory history =
            account.filterByDateRange(rawTransactions, query.dateRange()); // Domain 執行

        // Step 5：轉換為 Read Model（Application Layer 職責）
        return TwdTransactionHistoryResult.from(history, query.page(), query.size());
    }
}

// ── GetFxTransactionHistoryHandler ───────────────────────────────
@Component
public class GetFxTransactionHistoryHandler implements GetFxTransactionHistoryUseCase {

    private final LoadAccountPort loadAccountPort;
    private final LoadTransactionPort loadTransactionPort;

    @Override
    public FxTransactionHistoryResult execute(GetFxTransactionHistoryQuery query) {

        Account account = loadAccountPort.findByAccountId(query.accountId())
            .orElseThrow(() -> new AccountNotFoundException(query.accountId()));

        account.verifyOwnership(query.customerId());
        account.ensureActive();

        // 幣別符合性由 Account Domain Method 驗證（filterByDateRange 內部）
        List<Transaction> rawTransactions =
            loadTransactionPort.findByAccountId(query.accountId(), query.dateRange());

        TransactionHistory history =
            account.filterByDateRange(rawTransactions, query.dateRange());

        return FxTransactionHistoryResult.from(history, query.currency(),
                                               query.page(), query.size());
    }
}

// ── GetTransferPrivilegeHandler ───────────────────────────────────
@Component
public class GetTransferPrivilegeHandler implements GetTransferPrivilegeUseCase {

    private final LoadPrivilegePort loadPrivilegePort;

    @Override
    public TransferPrivilegeResult execute(GetTransferPrivilegeQuery query) {

        List<TransferPrivilege> privileges =
            loadPrivilegePort.findByCustomerId(query.customerId());

        // 每個 Privilege Aggregate 自己計算業務狀態（isValid、getRemainingQuota）
        return TransferPrivilegeResult.from(privileges);
    }
}

// ── GetPrivilegeUsageHistoryHandler ──────────────────────────────
@Component
public class GetPrivilegeUsageHistoryHandler implements GetPrivilegeUsageHistoryUseCase {

    private final LoadPrivilegePort loadPrivilegePort;

    @Override
    public PrivilegeUsageHistoryResult execute(GetPrivilegeUsageHistoryQuery query) {

        TransferPrivilege privilege = loadPrivilegePort.findByPrivilegeId(query.privilegeId())
            .orElseThrow(() -> new PrivilegeNotFoundException(query.privilegeId()));

        // 委派所有權驗證至 Domain
        privilege.verifyOwnership(query.customerId());

        // 委派使用紀錄過濾至 Domain
        PrivilegeUsageHistory usageHistory =
            privilege.filterUsageHistory(query.dateRange());

        return PrivilegeUsageHistoryResult.from(usageHistory, query.page(), query.size());
    }
}
```

### 6.6 Read Models（application/query/result/）

```java
// 台幣交易紀錄 Read Model
public record TwdTransactionHistoryResult(
    String accountId,
    List<TwdTransactionDto> transactions,
    PageInfo pageInfo
) {
    public static TwdTransactionHistoryResult from(TransactionHistory history,
                                                    int page, int size) {
        var dtos = history.transactions().stream()
            .map(TwdTransactionDto::from)
            .toList();
        // 分頁邏輯在 Application Layer 執行（非業務規則，屬展示需求）
        return new TwdTransactionHistoryResult(
            history.accountId().value(),
            paginate(dtos, page, size),
            PageInfo.of(page, size, dtos.size())
        );
    }
}

public record TwdTransactionDto(
    String transactionId,
    String transactionDate,
    String transactionType,
    String amount,
    String description,
    String channel
) {
    public static TwdTransactionDto from(Transaction t) { ... }
}

// 外幣交易紀錄 Read Model（多呈現原幣/匯率/等值）
public record FxTransactionDto(
    String transactionId,
    String transactionDate,
    String transactionType,
    String currencyCode,
    String fxAmount,
    String twdEquivalent,
    String exchangeRate,
    String description
) {}

// 轉帳優惠 Read Model
public record TransferPrivilegeDto(
    String privilegeId,
    String privilegeType,
    String description,
    int totalQuota,
    int usedQuota,
    int remainingQuota,
    String validFrom,
    String validTo,
    boolean isValid
) {
    // 從 Domain Aggregate 轉換，讀取 Aggregate 計算後的值
    public static TransferPrivilegeDto from(TransferPrivilege p) {
        return new TransferPrivilegeDto(
            p.getPrivilegeId().value(),
            p.getType().name(),
            p.getType().description(),
            p.getTotalQuota(),
            p.getUsedQuota(),
            p.getRemainingQuota(),   // Domain Method
            p.getValidPeriod().startDate().toString(),
            p.getValidPeriod().endDate().toString(),
            p.isValid()              // Domain Method
        );
    }
}
```

---

## 7. API 設計規範

### 7.1 RESTful Endpoints

| Method | Path | 說明 |
|--------|------|------|
| `GET` | `/api/v1/accounts/{accountId}/transactions/twd` | 台幣活存交易紀錄 |
| `GET` | `/api/v1/accounts/{accountId}/transactions/fx` | 外幣活存交易紀錄 |
| `GET` | `/api/v1/customers/me/privileges/transfer` | 轉帳優惠內容查詢 |
| `GET` | `/api/v1/customers/me/privileges/transfer/{privilegeId}/usage` | 優惠使用紀錄 |

### 7.2 查詢參數規範

```yaml
GET /api/v1/accounts/{accountId}/transactions/twd
Parameters:
  - startDate: date (YYYY-MM-DD, required)
  - endDate:   date (YYYY-MM-DD, required)
  - page:      integer (default: 0)
  - size:      integer (default: 20, max: 100)

GET /api/v1/accounts/{accountId}/transactions/fx
Parameters:
  - currency:  string (ISO 4217, required, e.g. USD / JPY / EUR)
  - startDate: date (YYYY-MM-DD, required)
  - endDate:   date (YYYY-MM-DD, required)
  - page:      integer (default: 0)
  - size:      integer (default: 20, max: 100)

GET /api/v1/customers/me/privileges/transfer/{privilegeId}/usage
Parameters:
  - startDate: date (YYYY-MM-DD, required)
  - endDate:   date (YYYY-MM-DD, required)
  - page:      integer (default: 0)
  - size:      integer (default: 20, max: 100)
```

### 7.3 統一回應格式

```json
// 成功 200 OK
{
  "code": "SUCCESS",
  "data": { ... },
  "timestamp": "2025-01-15T10:30:00+08:00"
}

// 業務規則違反 422
{
  "code": "QUERY_RANGE_EXCEEDED",
  "message": "查詢區間不可超過 13 個月",
  "timestamp": "2025-01-15T10:30:00+08:00"
}

// 授權失敗 403
{
  "code": "ACCOUNT_NOT_OWNED_BY_CUSTOMER",
  "message": "帳戶不屬於此客戶",
  "timestamp": "2025-01-15T10:30:00+08:00"
}
```

### 7.4 HTTP 狀態碼規範

| 狀態碼 | 情境 |
|--------|------|
| `200` | 查詢成功 |
| `400` | 請求參數錯誤（日期格式錯誤、幣別不支援） |
| `401` | JWT 未提供或無效 |
| `403` | 帳戶不屬於目前認證客戶（Domain Exception 對應） |
| `404` | 帳戶或優惠不存在 |
| `422` | 業務規則違反（查詢區間超過 13 個月） |
| `500` | 系統錯誤 |

### 7.5 Driving Adapter — REST Controller

```java
// AccountController — Driving Adapter
// 職責：HTTP 轉換（Request → Query Object，Result → HTTP Response）
// 不含任何業務邏輯
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final GetTwdTransactionHistoryUseCase getTwdTransactionHistory;
    private final GetFxTransactionHistoryUseCase getFxTransactionHistory;

    // 從 JWT SecurityContext 取得 CustomerId（Spring Security 注入）
    @GetMapping("/{accountId}/transactions/twd")
    public ResponseEntity<ApiResponse<TwdTransactionHistoryResult>> getTwdTransactions(
        @PathVariable String accountId,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal JwtPrincipal principal
    ) {
        var query = new GetTwdTransactionHistoryQuery(
            CustomerId.of(principal.getCustomerId()),
            new AccountId(accountId),
            new DateRange(startDate, endDate),
            page, size
        );
        var result = getTwdTransactionHistory.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

---

## 8. TDD 設計規劃

### 8.1 測試策略（由內而外）

```
Domain Layer Tests      →  Application Layer Tests  →  Adapter Tests  →  E2E / BDD
(JUnit 5, 純 Java)         (JUnit 5 + Mockito)         (Spring Test)      (Cucumber)
無任何 Mock                 Mock Output Ports            @WebMvcTest        Testcontainers
最快速、最純粹              驗證協調流程                 @DataJpaTest
```

### 8.2 Domain Layer 單元測試

#### 8.2.1 Value Object 測試

```java
class MoneyTest {

    @Test
    @DisplayName("相同幣別相加應回傳正確金額")
    void should_add_two_money_with_same_currency() {
        var m1 = Money.twd(new BigDecimal("1000.00"));
        var m2 = Money.twd(new BigDecimal("500.00"));

        var result = m1.add(m2);

        assertThat(result.amount()).isEqualByComparingTo("1500.00");
        assertThat(result.currency()).isEqualTo(Currency.TWD);
    }

    @Test
    @DisplayName("不同幣別相加應拋出 CurrencyMismatchException")
    void should_throw_when_adding_different_currencies() {
        var twd = Money.twd(new BigDecimal("1000"));
        var usd = new Money(new BigDecimal("30"), Currency.USD);

        assertThatThrownBy(() -> twd.add(usd))
            .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("負數金額應拋出 IllegalArgumentException")
    void should_throw_when_amount_is_negative() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), Currency.TWD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("金額不可為負數");
    }
}

class DateRangeTest {

    @Test
    @DisplayName("查詢區間超過 13 個月應回傳 true")
    void should_return_true_when_range_exceeds_13_months() {
        var range = new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 3, 1));

        assertThat(range.exceedsMonths(13)).isTrue();
    }

    @Test
    @DisplayName("startDate 晚於 endDate 應拋出例外")
    void should_throw_when_start_is_after_end() {
        assertThatThrownBy(() ->
            new DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 1, 1)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

#### 8.2.2 Account Aggregate 測試

```java
class AccountTest {

    @Test
    @DisplayName("帳戶持有人呼叫 verifyOwnership 應通過")
    void should_pass_when_owner_verifies_ownership() {
        var ownerId = CustomerId.of("C001");
        var account = AccountTestFixture.activeTwdAccount(ownerId);

        assertThatCode(() -> account.verifyOwnership(ownerId))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("非持有人呼叫 verifyOwnership 應拋出 AccountNotOwnedByCustomerException")
    void should_throw_when_non_owner_verifies_ownership() {
        var owner = CustomerId.of("C001");
        var intruder = CustomerId.of("C999");
        var account = AccountTestFixture.activeTwdAccount(owner);

        assertThatThrownBy(() -> account.verifyOwnership(intruder))
            .isInstanceOf(AccountNotOwnedByCustomerException.class);
    }

    @Test
    @DisplayName("filterByDateRange 超過 13 個月應拋出 QueryRangeExceededException")
    void should_throw_when_query_range_exceeds_13_months() {
        var account = AccountTestFixture.activeTwdAccount();
        var transactions = Collections.<Transaction>emptyList();
        var invalidRange = new DateRange(
            LocalDate.now().minusMonths(14), LocalDate.now());

        assertThatThrownBy(() -> account.filterByDateRange(transactions, invalidRange))
            .isInstanceOf(QueryRangeExceededException.class)
            .hasMessageContaining("13 個月");
    }

    @Test
    @DisplayName("filterByDateRange 應只回傳區間內的交易")
    void should_return_only_transactions_within_date_range() {
        var account = AccountTestFixture.activeTwdAccount();
        var range = new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        var transactions = List.of(
            TransactionTestFixture.on(LocalDate.of(2025, 1, 10)),
            TransactionTestFixture.on(LocalDate.of(2025, 2, 5))  // 區間外
        );

        var history = account.filterByDateRange(transactions, range);

        assertThat(history.transactions()).hasSize(1);
        assertThat(history.transactions().get(0).transactionDate().toLocalDate())
            .isEqualTo(LocalDate.of(2025, 1, 10));
    }
}
```

#### 8.2.3 TransferPrivilege Aggregate 測試

```java
class TransferPrivilegeTest {

    @Test
    @DisplayName("有效期內且有剩餘次數的優惠應回傳 isValid = true")
    void should_return_true_when_privilege_is_valid() {
        var privilege = PrivilegeTestFixture.activePrivilege(totalQuota: 10, usedQuota: 3);

        assertThat(privilege.isValid()).isTrue();
        assertThat(privilege.getRemainingQuota()).isEqualTo(7);
    }

    @Test
    @DisplayName("已使用完次數的優惠應回傳 isValid = false")
    void should_return_false_when_quota_exhausted() {
        var privilege = PrivilegeTestFixture.activePrivilege(totalQuota: 5, usedQuota: 5);

        assertThat(privilege.isValid()).isFalse();
        assertThat(privilege.getRemainingQuota()).isZero();
    }

    @Test
    @DisplayName("filterUsageHistory 應只回傳區間內的使用紀錄")
    void should_filter_usage_records_by_date_range() {
        var privilege = PrivilegeTestFixture.privilegeWithUsageRecords();
        var range = new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        var history = privilege.filterUsageHistory(range);

        assertThat(history.records())
            .allMatch(r -> range.contains(r.usedDate()));
    }
}
```

### 8.3 Application Layer 測試（Mockito — Mock Output Ports）

```java
@ExtendWith(MockitoExtension.class)
class GetTwdTransactionHistoryHandlerTest {

    // Mock Output Ports（不 Mock Domain Objects）
    @Mock private LoadAccountPort loadAccountPort;
    @Mock private LoadTransactionPort loadTransactionPort;
    @InjectMocks private GetTwdTransactionHistoryHandler handler;

    @Test
    @DisplayName("成功查詢台幣交易紀錄")
    void should_return_twd_transaction_history_successfully() {
        // Given
        var query = QueryFixture.twdQuery("C001", "00123456789012");
        var mockAccount = AccountTestFixture.activeTwdAccount(CustomerId.of("C001"));
        var mockTransactions = TransactionTestFixture.sampleList();

        given(loadAccountPort.findByAccountId(any())).willReturn(Optional.of(mockAccount));
        given(loadTransactionPort.findByAccountId(any(), any())).willReturn(mockTransactions);

        // When
        var result = handler.execute(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.transactions()).isNotEmpty();
        // 驗證 Output Port 被正確呼叫
        then(loadAccountPort).should().findByAccountId(new AccountId("00123456789012"));
        then(loadTransactionPort).should().findByAccountId(any(), any());
    }

    @Test
    @DisplayName("帳戶不存在應拋出 AccountNotFoundException")
    void should_throw_when_account_not_found() {
        given(loadAccountPort.findByAccountId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(QueryFixture.twdQuery("C001", "00123456789012")))
            .isInstanceOf(AccountNotFoundException.class);

        // 帳戶不存在時，不應查詢交易
        then(loadTransactionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("非帳戶持有人查詢應拋出 AccountNotOwnedByCustomerException")
    void should_throw_when_not_account_owner() {
        // Account 持有人為 C001，查詢者為 C999
        var mockAccount = AccountTestFixture.activeTwdAccount(CustomerId.of("C001"));
        given(loadAccountPort.findByAccountId(any())).willReturn(Optional.of(mockAccount));

        var query = QueryFixture.twdQuery("C999", "00123456789012");  // 非持有人

        assertThatThrownBy(() -> handler.execute(query))
            .isInstanceOf(AccountNotOwnedByCustomerException.class);

        // 所有權驗證失敗後，不應查詢交易
        then(loadTransactionPort).shouldHaveNoInteractions();
    }
}
```

### 8.4 Driving Adapter 測試（@WebMvcTest）

```java
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean GetTwdTransactionHistoryUseCase getTwdTransactionHistory;

    @Test
    @DisplayName("成功查詢 — 回傳 200 與交易清單")
    @WithMockJwtUser(customerId = "C001")
    void should_return_200_with_transactions() throws Exception {
        given(getTwdTransactionHistory.execute(any()))
            .willReturn(TwdTransactionHistoryResultFixture.sample());

        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.transactions").isArray());
    }

    @Test
    @DisplayName("缺少 startDate — 回傳 400")
    void should_return_400_when_startDate_missing() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("非帳戶持有人 — Controller 層回傳 403")
    @WithMockJwtUser(customerId = "C999")
    void should_return_403_when_not_owner() throws Exception {
        given(getTwdTransactionHistory.execute(any()))
            .willThrow(new AccountNotOwnedByCustomerException(
                new AccountId("00123456789012"), CustomerId.of("C999")));

        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_OWNED_BY_CUSTOMER"));
    }
}
```

---

## 9. BDD 設計規劃

### 9.1 工具選型

| 工具 | 用途 |
|------|------|
| **Cucumber 7** | BDD Framework |
| **Gherkin** | Feature 情境語言（繁體中文） |
| **Spring Boot Test** | Integration Test Context |
| **Testcontainers** | PostgreSQL / Redis 真實容器 |
| **WireMock** | Mock Core Banking 外部 API |

### 9.2 Feature 文件

#### 9.2.1 台幣交易紀錄查詢

```gherkin
# src/test/resources/features/account/twd_transaction_history.feature
# language: zh-TW
Feature: 台幣活存帳戶交易紀錄查詢
  作為一位已認證的銀行客戶
  我想要查詢我的台幣活存帳戶交易紀錄
  以便了解帳戶資金進出狀況

  Background:
    Given 客戶 "C001" 已完成身份認證
    And 客戶 "C001" 名下有台幣帳戶 "00123456789012"
    And 帳戶 "00123456789012" 在 2025 年 1 月有以下交易:
      | 日期       | 類型 | 金額  | 說明     |
      | 2025-01-05 | 存入 | 50000 | 薪資轉帳 |
      | 2025-01-10 | 提出 | 10000 | ATM 提款 |
      | 2025-01-20 | 存入 | 3000  | 利息入帳 |

  Scenario: 成功查詢指定日期區間的交易紀錄
    When 客戶查詢帳戶 "00123456789012" 從 "2025-01-01" 到 "2025-01-31" 的台幣交易紀錄
    Then 回應狀態碼為 200
    And 應回傳 3 筆交易紀錄
    And 第一筆交易類型為 "存入" 且金額為 "50000"

  Scenario: 查詢超過 13 個月區間應失敗
    When 客戶查詢帳戶 "00123456789012" 從 "2023-12-01" 到 "2025-02-01" 的台幣交易紀錄
    Then 回應狀態碼為 422
    And 錯誤代碼為 "QUERY_RANGE_EXCEEDED"
    And 錯誤訊息包含 "13 個月"

  Scenario: 查詢不屬於自己的帳戶應被拒絕
    Given 帳戶 "00999999999999" 屬於其他客戶
    When 客戶 "C001" 嘗試查詢帳戶 "00999999999999" 的台幣交易紀錄
    Then 回應狀態碼為 403
    And 錯誤代碼為 "ACCOUNT_NOT_OWNED_BY_CUSTOMER"

  Scenario: 帳戶狀態為凍結時查詢應失敗
    Given 帳戶 "00123456789012" 狀態為 "凍結"
    When 客戶查詢帳戶 "00123456789012" 從 "2025-01-01" 到 "2025-01-31" 的台幣交易紀錄
    Then 回應狀態碼為 422
    And 錯誤代碼為 "ACCOUNT_NOT_ACTIVE"
```

#### 9.2.2 外幣交易紀錄查詢

```gherkin
# src/test/resources/features/account/fx_transaction_history.feature
Feature: 外幣活存帳戶交易紀錄查詢
  作為一位已認證的銀行客戶
  我想要依幣別查詢我的外幣活存帳戶交易紀錄
  以便掌握外幣資產的變動情形

  Background:
    Given 客戶 "C001" 已完成身份認證
    And 客戶 "C001" 有美元帳戶 "00123456789013"

  Scenario: 成功查詢美元交易紀錄並顯示台幣等值
    Given 帳戶 "00123456789013" 在 2025 年 1 月有以下外幣交易:
      | 日期       | 類型 | 原幣金額 | 幣別 | 匯率 |
      | 2025-01-05 | 存入 | 1000.00  | USD  | 32.5 |
    When 客戶查詢帳戶 "00123456789013" 幣別 "USD" 從 "2025-01-01" 到 "2025-01-31" 的外幣交易紀錄
    Then 回應狀態碼為 200
    And 交易紀錄應包含原幣金額 "1000.00 USD"
    And 交易紀錄應包含台幣等值 "32500.00 TWD"
    And 交易紀錄應顯示匯率 "32.5"

  Scenario: 查詢不支援的幣別應回傳 400
    When 客戶查詢帳戶 "00123456789013" 幣別 "XXX" 從 "2025-01-01" 到 "2025-01-31" 的外幣交易紀錄
    Then 回應狀態碼為 400
    And 錯誤代碼為 "UNSUPPORTED_CURRENCY"
```

#### 9.2.3 轉帳優惠查詢

```gherkin
# src/test/resources/features/privilege/transfer_privilege.feature
Feature: 轉帳優惠查詢
  作為一位已認證的銀行客戶
  我想要查詢目前可用的轉帳優惠內容與使用紀錄
  以便節省轉帳手續費並追蹤使用情況

  Background:
    Given 客戶 "C001" 已完成身份認證

  Scenario: 成功查詢有效的轉帳優惠
    Given 客戶 "C001" 有以下轉帳優惠:
      | 優惠ID | 優惠類型       | 總次數 | 已用次數 | 有效期起     | 有效期訖     |
      | P001   | 免手續費跨行轉帳 | 10    | 3       | 2025-01-01  | 2025-12-31  |
    When 客戶查詢轉帳優惠內容
    Then 回應狀態碼為 200
    And 優惠 "P001" 剩餘次數為 7
    And 優惠 "P001" 狀態為有效

  Scenario: 已過期優惠應顯示無效
    Given 客戶 "C001" 有以下轉帳優惠:
      | 優惠ID | 優惠類型       | 總次數 | 已用次數 | 有效期起     | 有效期訖     |
      | P002   | 免手續費跨行轉帳 | 5     | 2       | 2024-01-01  | 2024-12-31  |
    When 客戶查詢轉帳優惠內容
    Then 優惠 "P002" 狀態為已過期

  Scenario: 成功查詢優惠使用紀錄
    Given 客戶 "C001" 的優惠 "P001" 有以下使用紀錄:
      | 使用日期   | 節省金額 | 轉帳目標帳號        |
      | 2025-01-05 | 30      | 81234567890123 |
      | 2025-01-12 | 30      | 91111222333444 |
    When 客戶查詢優惠 "P001" 從 "2025-01-01" 到 "2025-01-31" 的使用紀錄
    Then 回應狀態碼為 200
    And 應回傳 2 筆使用紀錄
    And 總節省金額為 60 元

  Scenario: 查詢不屬於自己的優惠使用紀錄應被拒絕
    Given 優惠 "P999" 屬於其他客戶
    When 客戶 "C001" 嘗試查詢優惠 "P999" 的使用紀錄
    Then 回應狀態碼為 403
    And 錯誤代碼為 "PRIVILEGE_NOT_OWNED_BY_CUSTOMER"
```

### 9.3 Step Definitions 架構

```java
// bdd/steps/AccountSteps.java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class AccountSteps {

    @Autowired TestRestTemplate restTemplate;
    @Autowired AccountTestDataSetup testDataSetup;

    private ResponseEntity<String> response;

    @Given("客戶 {string} 已完成身份認證")
    public void customerIsAuthenticated(String customerId) {
        // 設定 JWT Token 至 TestRestTemplate header
        restTemplate.getRestTemplate().getInterceptors().add(
            new JwtAuthInterceptor(JwtTestTokenFactory.forCustomer(customerId))
        );
    }

    @Given("帳戶 {string} 在 {int} 年 {int} 月有以下交易:")
    public void accountHasTransactions(String accountId, int year, int month,
                                        DataTable dataTable) {
        testDataSetup.insertTransactions(accountId, year, month, dataTable.asMaps());
    }

    @When("客戶查詢帳戶 {string} 從 {string} 到 {string} 的台幣交易紀錄")
    public void queryTwdTransactions(String accountId, String startDate, String endDate) {
        response = restTemplate.getForEntity(
            "/api/v1/accounts/{accountId}/transactions/twd?startDate={start}&endDate={end}",
            String.class, accountId, startDate, endDate
        );
    }

    @Then("回應狀態碼為 {int}")
    public void verifyStatusCode(int expectedCode) {
        assertThat(response.getStatusCode().value()).isEqualTo(expectedCode);
    }

    @Then("應回傳 {int} 筆交易紀錄")
    public void verifyTransactionCount(int expectedCount) {
        var count = JsonPath.parse(response.getBody())
            .<List<?>>read("$.data.transactions").size();
        assertThat(count).isEqualTo(expectedCount);
    }

    @Then("錯誤代碼為 {string}")
    public void verifyErrorCode(String expectedCode) {
        var code = JsonPath.parse(response.getBody()).<String>read("$.code");
        assertThat(code).isEqualTo(expectedCode);
    }
}
```

---

## 10. 專案結構

```
banking-account-query-service/
├── src/
│   ├── main/
│   │   └── java/com/bank/accountquery/
│   │       │
│   │       ├── domain/                          ← 純粹核心，零外部依賴
│   │       │   ├── model/
│   │       │   │   ├── account/
│   │       │   │   │   ├── Account.java                  # Aggregate Root
│   │       │   │   │   ├── AccountId.java                # Value Object
│   │       │   │   │   ├── AccountType.java              # Enum (TWD/FX)
│   │       │   │   │   ├── AccountStatus.java            # Enum (ACTIVE/FROZEN/CLOSED)
│   │       │   │   │   ├── Transaction.java              # Entity
│   │       │   │   │   ├── TransactionId.java            # Value Object
│   │       │   │   │   ├── TransactionType.java          # Enum (CREDIT/DEBIT)
│   │       │   │   │   ├── TransactionChannel.java       # Enum
│   │       │   │   │   └── TransactionHistory.java       # Value Object（查詢結果）
│   │       │   │   ├── privilege/
│   │       │   │   │   ├── TransferPrivilege.java        # Aggregate Root
│   │       │   │   │   ├── PrivilegeId.java              # Value Object
│   │       │   │   │   ├── PrivilegeType.java            # Enum
│   │       │   │   │   ├── PrivilegeUsageRecord.java     # Entity
│   │       │   │   │   └── PrivilegeUsageHistory.java    # Value Object（查詢結果）
│   │       │   │   └── shared/
│   │       │   │       ├── Money.java                    # Value Object
│   │       │   │       ├── Currency.java                 # Enum / Value Object
│   │       │   │       ├── DateRange.java                # Value Object
│   │       │   │       └── CustomerId.java               # Value Object
│   │       │   └── exception/                            # Domain Exceptions（業務語意）
│   │       │       ├── AccountNotFoundException.java
│   │       │       ├── AccountNotOwnedByCustomerException.java
│   │       │       ├── AccountNotActiveException.java
│   │       │       ├── QueryRangeExceededException.java
│   │       │       ├── CurrencyMismatchException.java
│   │       │       ├── PrivilegeNotFoundException.java
│   │       │       └── PrivilegeNotOwnedByCustomerException.java
│   │       │
│   │       ├── application/                     ← Use Cases + Port 定義
│   │       │   ├── port/
│   │       │   │   ├── in/                               # Input Port（Use Case Interfaces）
│   │       │   │   │   ├── GetTwdTransactionHistoryUseCase.java
│   │       │   │   │   ├── GetFxTransactionHistoryUseCase.java
│   │       │   │   │   ├── GetTransferPrivilegeUseCase.java
│   │       │   │   │   └── GetPrivilegeUsageHistoryUseCase.java
│   │       │   │   └── out/                              # Output Port（由 Adapter 實作）
│   │       │   │       ├── LoadAccountPort.java          ← Repository Interface 在此！
│   │       │   │       ├── LoadTransactionPort.java
│   │       │   │       ├── LoadPrivilegePort.java
│   │       │   │       └── LoadPrivilegeUsagePort.java
│   │       │   └── query/
│   │       │       ├── account/
│   │       │       │   ├── GetTwdTransactionHistoryQuery.java    # Query Object
│   │       │       │   ├── GetTwdTransactionHistoryHandler.java  # Handler（實作 Input Port）
│   │       │       │   ├── GetFxTransactionHistoryQuery.java
│   │       │       │   ├── GetFxTransactionHistoryHandler.java
│   │       │       │   └── result/
│   │       │       │       ├── TwdTransactionHistoryResult.java  # Read Model
│   │       │       │       ├── TwdTransactionDto.java
│   │       │       │       ├── FxTransactionHistoryResult.java   # Read Model
│   │       │       │       ├── FxTransactionDto.java
│   │       │       │       └── PageInfo.java
│   │       │       └── privilege/
│   │       │           ├── GetTransferPrivilegeQuery.java
│   │       │           ├── GetTransferPrivilegeHandler.java
│   │       │           ├── GetPrivilegeUsageHistoryQuery.java
│   │       │           ├── GetPrivilegeUsageHistoryHandler.java
│   │       │           └── result/
│   │       │               ├── TransferPrivilegeResult.java      # Read Model
│   │       │               ├── TransferPrivilegeDto.java
│   │       │               ├── PrivilegeUsageHistoryResult.java  # Read Model
│   │       │               └── PrivilegeUsageDto.java
│   │       │
│   │       └── infrastructure/                  ← Adapters（實作 Ports）
│   │           ├── adapter/
│   │           │   ├── in/rest/                          # Driving Adapters
│   │           │   │   ├── AccountController.java        # 呼叫 Input Port
│   │           │   │   ├── PrivilegeController.java
│   │           │   │   ├── GlobalExceptionHandler.java   # @ControllerAdvice
│   │           │   │   └── dto/
│   │           │   │       └── ApiResponse.java
│   │           │   └── out/                              # Driven Adapters（實作 Output Port）
│   │           │       ├── persistence/
│   │           │       │   ├── AccountJpaAdapter.java    # 實作 LoadAccountPort
│   │           │       │   ├── TransactionJpaAdapter.java # 實作 LoadTransactionPort
│   │           │       │   ├── PrivilegeJpaAdapter.java  # 實作 LoadPrivilegePort
│   │           │       │   └── entity/                   # JPA Entities（僅在 infra 層）
│   │           │       │       ├── AccountEntity.java
│   │           │       │       ├── TransactionEntity.java
│   │           │       │       └── PrivilegeEntity.java
│   │           │       ├── corebanking/
│   │           │       │   └── CoreBankingAdapter.java
│   │           │       └── cache/
│   │           │           └── PrivilegeCacheAdapter.java # 實作 LoadPrivilegePort（裝飾模式）
│   │           └── config/
│   │               ├── SecurityConfig.java
│   │               ├── CacheConfig.java
│   │               └── OpenApiConfig.java
│   │
│   └── test/
│       ├── java/com/bank/accountquery/
│       │   ├── domain/                          # Domain Unit Tests（純 Java）
│       │   │   ├── model/account/
│       │   │   │   ├── AccountTest.java
│       │   │   │   ├── TransactionHistoryTest.java
│       │   │   │   └── MoneyTest.java
│       │   │   ├── model/privilege/
│       │   │   │   └── TransferPrivilegeTest.java
│       │   │   └── model/shared/
│       │   │       └── DateRangeTest.java
│       │   ├── application/                     # Application Unit Tests（Mockito）
│       │   │   └── query/
│       │   │       ├── GetTwdTransactionHistoryHandlerTest.java
│       │   │       ├── GetFxTransactionHistoryHandlerTest.java
│       │   │       ├── GetTransferPrivilegeHandlerTest.java
│       │   │       └── GetPrivilegeUsageHistoryHandlerTest.java
│       │   ├── infrastructure/                  # Adapter Tests
│       │   │   ├── adapter/in/rest/
│       │   │   │   ├── AccountControllerTest.java      # @WebMvcTest
│       │   │   │   └── PrivilegeControllerTest.java
│       │   │   └── adapter/out/persistence/
│       │   │       └── AccountJpaAdapterTest.java      # @DataJpaTest + Testcontainers
│       │   ├── bdd/                             # BDD Integration Tests
│       │   │   ├── CucumberIntegrationTest.java        # @Suite Entry Point
│       │   │   ├── steps/
│       │   │   │   ├── AccountSteps.java
│       │   │   │   └── PrivilegeSteps.java
│       │   │   └── support/
│       │   │       ├── AccountTestDataSetup.java
│       │   │       └── JwtTestTokenFactory.java
│       │   └── fixture/                         # 共用測試夾具
│       │       ├── AccountTestFixture.java
│       │       ├── TransactionTestFixture.java
│       │       └── PrivilegeTestFixture.java
│       └── resources/
│           └── features/
│               ├── account/
│               │   ├── twd_transaction_history.feature
│               │   └── fx_transaction_history.feature
│               └── privilege/
│                   ├── transfer_privilege.feature
│                   └── privilege_usage_history.feature
│
├── build.gradle.kts
└── README.md
```

---

## 11. 工項清單 Work Breakdown Structure

### Sprint 0 — 環境建置（1 週）

| 工項 ID | 工項名稱 | 說明 | 人天 |
|---------|----------|------|------|
| S0-01 | 建立專案骨架 | Spring Boot 4 + Hexagonal 目錄結構 | 0.5 |
| S0-02 | 設定 build.gradle.kts | Java 23、Cucumber、Testcontainers 依賴 | 0.5 |
| S0-03 | Testcontainers 設定 | PostgreSQL + Redis 容器 | 1.0 |
| S0-04 | WireMock 設定 | Mock Core Banking API | 0.5 |
| S0-05 | CI Pipeline | GitHub Actions，含測試報告 | 1.0 |
| S0-06 | OpenAPI 設定 | springdoc-openapi 整合 | 0.5 |

### Sprint 1 — Domain Layer（2 週）

| 工項 ID | 工項名稱 | 說明 | 人天 |
|---------|----------|------|------|
| S1-01 | Shared Value Objects | Money、DateRange、CustomerId、Currency | 1.0 |
| S1-02 | AccountId Value Object | 含 14 位數字格式驗證 | 0.5 |
| S1-03 | Transaction Entity | 含 TransactionId、Money、Channel | 0.5 |
| S1-04 | TransactionHistory Value Object | 防禦性複製，不可變 | 0.5 |
| S1-05 | Account Aggregate Root | verifyOwnership、ensureActive、filterByDateRange | 1.5 |
| S1-06 | PrivilegeUsageRecord Entity | 含使用日期、節省金額 | 0.5 |
| S1-07 | TransferPrivilege Aggregate Root | isValid、getRemainingQuota、filterUsageHistory | 1.5 |
| S1-08 | Domain Exceptions | 所有業務語意例外類別 | 0.5 |
| S1-09 | Domain Layer 單元測試 | Value Object + Aggregate 完整 TDD | 2.5 |

### Sprint 2 — Application Layer（1.5 週）

| 工項 ID | 工項名稱 | 說明 | 人天 |
|---------|----------|------|------|
| S2-01 | Output Port Interfaces | LoadAccountPort、LoadTransactionPort、LoadPrivilegePort、LoadPrivilegeUsagePort | 0.5 |
| S2-02 | Input Port Interfaces | 4 個 UseCase Interfaces | 0.5 |
| S2-03 | Query Objects | 4 個 record Query Objects | 0.5 |
| S2-04 | GetTwdTransactionHistoryHandler | 含 Read Model 轉換 | 1.0 |
| S2-05 | GetFxTransactionHistoryHandler | 含幣別呈現邏輯 | 1.0 |
| S2-06 | GetTransferPrivilegeHandler | 含 isValid 狀態映射 | 0.5 |
| S2-07 | GetPrivilegeUsageHistoryHandler | 含使用紀錄分頁 | 0.5 |
| S2-08 | Read Models（DTOs）| 8 個 Result / DTO record | 1.0 |
| S2-09 | Application Layer 單元測試 | Mockito Mock Output Ports，4 個 Handler | 2.0 |

### Sprint 3 — Infrastructure Adapters（1.5 週）

| 工項 ID | 工項名稱 | 說明 | 人天 |
|---------|----------|------|------|
| S3-01 | AccountController（Driving Adapter）| 2 個端點、參數驗證 | 1.5 |
| S3-02 | PrivilegeController（Driving Adapter）| 2 個端點 | 1.0 |
| S3-03 | GlobalExceptionHandler | Domain Exception → HTTP Status 映射 | 1.0 |
| S3-04 | JPA Entity Schema 設計 | Account、Transaction、Privilege 資料表 | 1.0 |
| S3-05 | AccountJpaAdapter（Driven Adapter）| 實作 LoadAccountPort | 1.0 |
| S3-06 | TransactionJpaAdapter（Driven Adapter）| 實作 LoadTransactionPort，含分頁 | 1.0 |
| S3-07 | PrivilegeJpaAdapter（Driven Adapter）| 實作 LoadPrivilegePort | 0.5 |
| S3-08 | CoreBankingAdapter | 串接 Core Banking，含 WireMock 驗證 | 1.5 |
| S3-09 | PrivilegeCacheAdapter | Redis 快取，Decorator Pattern | 1.0 |
| S3-10 | Controller 單元測試（@WebMvcTest）| 4 個端點完整測試 | 1.5 |
| S3-11 | JPA Adapter 整合測試（@DataJpaTest）| Testcontainers PostgreSQL | 1.0 |

### Sprint 4 — BDD Integration Tests（1 週）

| 工項 ID | 工項名稱 | 說明 | 人天 |
|---------|----------|------|------|
| S4-01 | 台幣交易紀錄 Feature 文件 | 含 Error Path | 0.5 |
| S4-02 | 外幣交易紀錄 Feature 文件 | 含幣別驗證情境 | 0.5 |
| S4-03 | 轉帳優惠查詢 Feature 文件 | 含過期/額度用盡情境 | 0.5 |
| S4-04 | 優惠使用紀錄 Feature 文件 | 含跨客戶越權情境 | 0.5 |
| S4-05 | AccountSteps 實作 | Step Definitions | 1.5 |
| S4-06 | PrivilegeSteps 實作 | Step Definitions | 1.0 |
| S4-07 | Test Fixture + TestDataSetup | Builder Pattern 測試資料建立 | 1.0 |
| S4-08 | BDD 報告整合 | Cucumber HTML Report 輸出至 CI | 0.5 |

### Sprint 5 — 安全性與觀測性（0.5 週）

| 工項 ID | 工項名稱 | 說明 | 人天 |
|---------|----------|------|------|
| S5-01 | JWT 認證整合 | Spring Security + CustomerId 提取 | 1.0 |
| S5-02 | Audit Log | AOP 攔截，記錄查詢稽核資訊 | 1.0 |
| S5-03 | Micrometer / Prometheus | 查詢延遲、錯誤率 Metrics | 0.5 |
| S5-04 | OpenAPI 文件補全 | 端點描述、範例值、錯誤碼說明 | 0.5 |

### 工項彙總

| Sprint | 主題 | 人天 |
|--------|------|------|
| Sprint 0 | 環境建置 | 4.0 |
| Sprint 1 | Domain Layer | 9.0 |
| Sprint 2 | Application Layer | 7.5 |
| Sprint 3 | Infrastructure Adapters | 11.0 |
| Sprint 4 | BDD Integration Tests | 6.0 |
| Sprint 5 | 安全性與觀測性 | 3.0 |
| **合計** | | **40.5 人天** |

---

## 12. 技術選型說明

### 12.1 核心依賴

```kotlin
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "4.0.x"
    id("io.spring.dependency-management") version "1.x"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(23) }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.x")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.x")

    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")          // DB Migration

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ── Test ──────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")  // JUnit 5 + Mockito
    testImplementation("io.cucumber:cucumber-java:7.x")
    testImplementation("io.cucumber:cucumber-spring:7.x")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.wiremock:wiremock-standalone:3.x")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.jayway.jsonpath:json-path")
}
```

### 12.2 Java 23 特性應用

| Java 特性 | 應用場景 |
|-----------|----------|
| **Record Classes** | Value Objects（Money、DateRange）、Query Objects、Read Model DTOs |
| **Sealed Classes** | `AccountType`（TWD / FX）、`PrivilegeType` 等有限狀態 |
| **Pattern Matching for instanceof** | `GlobalExceptionHandler` 中的例外類型判斷 |
| **Text Blocks** | SQL 測試資料、JSON 測試樣本 |
| **Virtual Threads（Project Loom）** | `spring.threads.virtual.enabled=true`，高並發 I/O 查詢 |

---

## 13. 非功能性需求考量

### 13.1 效能目標

| 指標 | 目標值 |
|------|--------|
| API 回應時間（P99） | < 500ms |
| API 回應時間（P95） | < 200ms |
| QPS 峰值 | ≥ 500 |
| 30 天交易紀錄查詢 | < 300ms |

### 13.2 安全性要求

- JWT Token 驗證（RS256 非對稱加密）
- 帳戶/優惠所有權驗證由 **Domain Aggregate 強制執行**（非 Application Layer 的 if 判斷）
- 所有查詢操作透過 AOP 記錄 Audit Log（IP、CustomerId、AccountId、Timestamp）
- HTTPS Only，TLS 1.2+

### 13.3 測試覆蓋率目標

| 層級 | 目標 |
|------|------|
| Domain Layer | ≥ 90%（業務規則最重要） |
| Application Layer | ≥ 85% |
| Infrastructure Adapters | ≥ 80% |
| BDD Scenarios | 涵蓋所有 Happy Path + 主要 Error Path |

### 13.4 快取策略

```
轉帳優惠內容（更新頻率低）
  → Redis Cache，TTL = 5 分鐘
  → Cache Key: privilege:customer:{customerId}
  → PrivilegeCacheAdapter 以 Decorator Pattern 包裝 PrivilegeJpaAdapter

台幣 / 外幣交易紀錄（即時性要求高）
  → 不快取，直接查詢 Read DB
  → 使用 DB Read Replica 減輕主庫壓力
```

---

## 14. 附錄：SOLID 原則對應表

| 原則 | 在本架構的體現 |
|------|---------------|
| **S 單一職責** | Account Aggregate 只負責帳戶業務規則；Handler 只負責協調流程；Controller 只負責 HTTP 轉換 |
| **O 開放封閉** | 新增幣別只需新增 Currency Enum；新增查詢功能只需新增 Query + Handler + Feature，不修改既有類別 |
| **L 里氏替換** | `PrivilegeCacheAdapter` 與 `PrivilegeJpaAdapter` 皆實作 `LoadPrivilegePort`，可互換而不影響 Handler |
| **I 介面隔離** | `LoadAccountPort` / `LoadTransactionPort` 各自獨立；Handler 只依賴自己需要的 Port，不被迫依賴用不到的方法 |
| **D 依賴倒置** | Handler 依賴 `LoadAccountPort`（Interface），不依賴 `AccountJpaAdapter`（實作）；Domain Layer 不依賴任何 Port 或 Framework |

### 依賴方向最終驗證

```
REST Client
    ↓
AccountController          (infrastructure/adapter/in/rest)
    ↓ 呼叫
GetTwdTransactionHistoryUseCase  (application/port/in)         ← Interface
    ↑ 實作
GetTwdTransactionHistoryHandler  (application/query)
    ↓ 呼叫
LoadAccountPort            (application/port/out)              ← Interface
    ↑ 實作
AccountJpaAdapter          (infrastructure/adapter/out/persistence)
    ↓ 存取
PostgreSQL

Domain Objects（Account、Money 等）
    ← 被 Handler 取得後，Handler 呼叫其 Domain Method
    ← Domain 本身不依賴任何人 ✅
```

---

## 15. ADR：Repository Pattern 設計決策

> **ADR = Architecture Decision Record**
> 記錄「為什麼這樣設計」，而非只記錄「設計是什麼」。
> 供後續維護者理解設計脈絡，避免在不清楚理由的情況下推翻決策。

---

### ADR-001：Repository 操作單位必須為 Aggregate Root

**狀態**：已採用（Accepted）

**背景**

DDD 的核心概念之一是 Aggregate，它定義了一個**一致性邊界（Consistency Boundary）**。
Aggregate Root 是邊界的守門人，所有對邊界內 Entity 的操作，都必須經由 Root 發起，
以確保業務不變式（Business Invariant）在每次操作後仍被滿足。

**決策**

Repository 的操作單位是 Aggregate Root，不為邊界內的 Entity 建立獨立的 Repository。

```java
// ✅ 正確：以 Aggregate Root 為單位
public interface SaveAccountPort {
    void save(Account account);         // 傳入整個 Aggregate
}

// ❌ 錯誤：繞過 Aggregate Root，直接操作內部 Entity
public interface SaveTransactionPort {
    void save(Transaction transaction); // Transaction 屬於 Account 邊界內
                                        // 不應有自己的 Repository
}
```

**為什麼 Write Side 必須傳入整個 Aggregate**

若允許直接 `save(Transaction)`，以下業務規則將無人守護：

```
帳戶凍結（AccountStatus.FROZEN）時，不得新增交易。

正確流程：
  account.addTransaction(...)     ← Account.ensureActive() 在此攔截
  saveAccountPort.save(account)   ← 存入整個 Aggregate

錯誤流程：
  saveTransactionPort.save(tx)    ← 直接繞過 Account，
                                     FROZEN 帳戶仍可寫入交易，
                                     Invariant 被破壞
```

**結論**

Write Side 的 Repository 傳入 Aggregate Root 是**不可妥協的硬規則**，
這是 DDD 中 Aggregate 能保護一致性的根本前提。

---

### ADR-002：Read Side Repository 的設計選項評估

**狀態**：已採用方案 A（Accepted — Option A）

**背景**

Read Side 理論上應回傳完整 Aggregate，讓業務規則（所有權驗證、狀態驗證）
仍由 Domain Model 執行。然而本系統面臨 **Large Aggregate 問題**：

```
銀行帳戶的交易紀錄量級：
一個活躍帳戶可能累積數萬筆交易紀錄

若 findByAccountId() 回傳含所有 Transaction 的完整 Account Aggregate：
→ 每次查詢帳戶資訊都載入數萬筆交易
→ 嚴重的記憶體壓力與查詢效能問題
→ 不符合銀行系統的效能 SLA（P95 < 200ms）
```

**評估的三個選項**

#### 方案 A — 分離查詢，Application Layer 協調，Domain 執行規則（本 Tutorial 採用）

```
Handler 流程：
  Step 1: LoadAccountPort.findByAccountId()
          → 取得 Account（含業務狀態，不含交易明細）
  Step 2: account.verifyOwnership(customerId)
          → Domain 執行所有權業務規則
  Step 3: account.ensureActive()
          → Domain 執行帳戶狀態業務規則
  Step 4: LoadTransactionPort.findByAccountId(accountId, dateRange)
          → 取得指定區間的原始交易資料（已在 DB 層過濾）
  Step 5: account.filterByDateRange(transactions, dateRange)
          → Domain 執行查詢區間限制規則（13 個月）與最終過濾
```

```
優點：
  ✅ 避免 Large Aggregate 載入問題
  ✅ 所有業務規則（所有權、帳戶狀態、查詢區間）仍由 Domain Model 執行
  ✅ DB 層已初步過濾資料，Application Layer 再交由 Domain 二次驗證

缺點：
  ⚠ Transaction 在 Step 4 暫時脫離 Aggregate 邊界
  ⚠ 需靠設計紀律確保：只有 Domain Method 可對 Transaction 執行業務判斷，
     Application Layer 不得直接對 Transaction List 進行業務邏輯判斷
```

#### 方案 B — Lazy Loading（讓 Aggregate 隱含載入機制）

```java
public class Account {
    private final Supplier<List<Transaction>> transactionsLoader; // 注入載入函式

    public TransactionHistory filterByDateRange(DateRange range) {
        var transactions = transactionsLoader.get(); // 需要時才觸發載入
        ...
    }
}
```

```
優點：
  ✅ Aggregate 對外保持完整介面，呼叫者無感知載入機制

缺點：
  ❌ Domain Model 隱含了對外部載入機制（Supplier / Repository）的依賴
  ❌ 違反 Domain Layer 純粹性：Domain 不應知道資料如何被取得
  ❌ 測試複雜度提高：需 Mock 載入函式才能測試 Domain Method
```

#### 方案 C — CQRS 徹底分離（Query Side 完全不走 Aggregate）

```
Command Side：Account Aggregate + Repository（Write，以 Aggregate 為單位）
Query Side：  Query Handler → Output Port → 直接查詢 Read DB → 回傳 DTO
             完全繞過 Domain Model

流程範例：
  Handler → LoadTwdTransactionReadModelPort.query(accountId, dateRange)
          → JPA / Native SQL 直接回傳 TwdTransactionDto
          → 無任何 Domain Object 參與
```

```
優點：
  ✅ 查詢效能最佳，可針對 Read DB 做客製化索引與 SQL 優化
  ✅ Aggregate 只承擔 Write Side 的一致性保護，職責更單純
  ✅ Read Model 可獨立演化，不受 Domain Model 結構限制

缺點：
  ❌ Query Side 無 Domain Model 保護：
     所有權驗證、帳戶狀態驗證必須在 Application Layer 以 if 判斷實作
     業務規則散落，維護成本提高
  ❌ 適合查詢複雜度高、讀寫模型差異大的場景，
     對本系統（查詢邏輯相對單純）而言過度設計
```

**決策矩陣**

| 評估項目 | 方案 A（採用）| 方案 B | 方案 C |
|----------|--------------|--------|--------|
| Domain 純粹性 | ✅ 維持 | ❌ 破壞 | ⚠ 放棄 |
| 業務規則集中 | ✅ Domain 執行 | ✅ Domain 執行 | ❌ Application Layer |
| 效能（Large Aggregate）| ✅ 分離查詢 | ✅ 延遲載入 | ✅ 最佳 |
| 測試難度 | ✅ 低 | ⚠ 中 | ⚠ 中 |
| 設計複雜度 | ✅ 低 | ⚠ 中 | ⚠ 中 |
| 適用時機 | 查詢邏輯含業務規則 | 需完整 Aggregate 介面 | 讀寫模型差異極大 |

**採用方案 A 的理由**

本系統的查詢操作含有明確業務規則（所有權驗證、帳戶狀態、13 個月限制），
這些規則應由 Domain Model 守護，不應下放至 Application Layer 的 if 判斷。
方案 A 在維持 Domain 規則執行權的前提下，有效解決 Large Aggregate 的效能問題，
是三個方案中**業務規則保護與系統效能之間最務實的平衡**。

**配套設計紀律（方案 A 的約束）**

採用方案 A 需明確訂立以下設計紀律，防止業務規則外洩：

```
規則 1：LoadTransactionPort 回傳的 List<Transaction>
        只可傳入 Domain Method（如 account.filterByDateRange(transactions, range)），
        Application Layer 不得直接對此 List 執行任何業務判斷。

規則 2：Handler 中若出現針對 Transaction 的 if / filter / stream 業務邏輯，
        視為 Code Smell，應重構回 Domain Method。

規則 3：Transaction 不建立獨立的 Repository 或 Output Port（Write Side）；
        LoadTransactionPort 僅供 Read Side 的 Handler 使用。
```

---

### ADR-003：Repository Interface 命名規範

**狀態**：已採用（Accepted）

**背景**

DDD 的 Repository Interface 命名慣例有兩種主流風格：

```
風格 1：技術導向命名
  AccountRepository
  TransactionRepository

風格 2：意圖導向命名（本 Tutorial 採用）
  LoadAccountPort
  SaveAccountPort
  LoadTransactionPort
```

**決策**

採用意圖導向命名（`LoadXxxPort` / `SaveXxxPort`），理由如下：

```
1. 語意清晰：
   名稱直接表達 Application Layer 的需求（「我需要載入帳戶」），
   不暗示資料從哪裡來（資料庫、快取、外部 API 皆可實作此 Port）。

2. 符合 Hexagonal Architecture 的 Port 語意：
   Port 是六角形的開口，代表「應用程式需要的能力」，
   用 Repository 命名會讓人誤以為一定有資料庫實作。

3. 支援多重實作而不混淆：
   LoadPrivilegePort 可同時被：
     - PrivilegeJpaAdapter（DB 實作）
     - PrivilegeCacheAdapter（Cache 實作，Decorator Pattern）
   若命名為 PrivilegeRepository，Cache 版本的命名就顯得奇怪。

4. 符合 ISP（Interface Segregation Principle）：
   LoadAccountPort 與 SaveAccountPort 分離，
   Handler 只依賴自己需要的能力，不被迫依賴 save() 等用不到的方法。
```

**命名對照表**

| 傳統命名 | 意圖導向命名 | 實作者（Driven Adapter）|
|----------|-------------|------------------------|
| `AccountRepository.findById()` | `LoadAccountPort` | `AccountJpaAdapter` |
| `AccountRepository.save()` | `SaveAccountPort` | `AccountJpaAdapter` |
| `TransactionRepository.findByAccountId()` | `LoadTransactionPort` | `TransactionJpaAdapter` |
| `PrivilegeRepository.findByCustomerId()` | `LoadPrivilegePort` | `PrivilegeJpaAdapter` / `PrivilegeCacheAdapter` |
