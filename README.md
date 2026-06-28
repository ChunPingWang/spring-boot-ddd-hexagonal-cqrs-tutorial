# 銀行帳戶查詢服務 Banking Account Query Service

一個小而**可實際執行**的銀行「帳戶查詢」API，用來把通常只出現在艱澀架構書籍裡的四個概念講清楚：

- **DDD（領域驅動設計）** — 把業務規則放進模型裡，而不是散落在各處程式碼。
- **Hexagonal Architecture（六角形架構／Ports & Adapters）** — 讓核心邏輯獨立於 Web、資料庫與框架之外。
- **CQRS（讀取側）** — 把「讀取資料」視為一條獨立、定義明確的路徑。
- **TDD** — 每一條規則都有快速、好讀的測試覆蓋。

它實作了 [`banking-api-tutorial-v2.md`](banking-api-tutorial-v2.md) 裡的真實功能：查詢台幣／外幣交易紀錄與轉帳優惠，
而且**只允許已登入的客戶查詢自己的資料**。

> 對這些名詞陌生？先跳到最下面的[名詞解釋](#名詞解釋)，再回來看。你不需要事先全部弄懂——下面的圖會自己說話。

---

## 目錄

1. [快速開始](#1-快速開始)
2. [一條解釋一切的規則：依賴方向規則](#2-一條解釋一切的規則依賴方向規則)
3. [三個層次](#3-三個層次)
4. [架構總覽（圖）](#4-架構總覽圖)
5. [類別圖 — 領域模型](#5-類別圖--領域模型)
6. [循序圖 — 一次請求發生了什麼](#6-循序圖--一次請求發生了什麼)
7. [ER 圖 — 資料如何關聯](#7-er-圖--資料如何關聯)
8. [設計決策說明](#8-設計決策說明)
9. [API](#9-api)
10. [錯誤如何轉換成 HTTP 狀態碼](#10-錯誤如何轉換成-http-狀態碼)
11. [測試策略](#11-測試策略)
12. [與 Tutorial 的差異](#12-與-tutorial-的差異)
13. [名詞解釋](#名詞解釋)

---

## 1. 快速開始

**先決條件：** 一套 JDK（Java 23 以上——本專案設定為 Java 25）。你**不需要**安裝 Gradle、Docker、PostgreSQL 或 Redis
——專案內含 Gradle wrapper，資料皆在記憶體中。

```bash
# 在 repo 根目錄執行
./gradlew test       # 執行全部 33 個測試（應全綠）
./gradlew bootRun    # 在 http://localhost:8080 啟動 API
```

接著開另一個終端機試打一個請求。身分認證以 `X-Customer-Id` Header 模擬（原因見[§8](#8-設計決策說明)）：

```bash
# 客戶 C001 查詢自己的台幣帳戶 — 成功
curl -H "X-Customer-Id: C001" \
  "http://localhost:8080/api/v1/accounts/00123456789012/transactions/twd?startDate=2025-01-01&endDate=2025-01-31"

# C001 嘗試查詢別人的帳戶 — 被領域模型擋下（HTTP 403）
curl -H "X-Customer-Id: C001" \
  "http://localhost:8080/api/v1/accounts/00999999999999/transactions/twd?startDate=2025-01-01&endDate=2025-01-31"
```

內建示範資料：客戶 **C001** 擁有台幣帳戶 `00123456789012` 與美元帳戶 `00123456789013`，以及優惠 `P001`（有效）與
`P002`（已過期）。帳戶 `00999999999999` 與優惠 `P999` 屬於另一位客戶。

---

## 2. 一條解釋一切的規則：依賴方向規則

幾乎每一個設計選擇都源自這條規則：

> **原始碼的依賴只能指向內層。** 內圈對外圈一無所知。

```mermaid
flowchart LR
    subgraph Infrastructure["基礎設施層 Infrastructure（最外層）"]
        direction TB
        REST["REST Controllers<br/>(Spring MVC)"]
        DB["持久化 Adapters<br/>(記憶體 / JPA)"]
    end
    subgraph Application["應用層 Application（中間層）"]
        direction TB
        HANDLER["Query Handlers"]
        PORTS["Ports（介面）"]
    end
    subgraph Domain["領域層 Domain（最內層，純粹）"]
        direction TB
        AGG["Aggregates + Value Objects<br/>（所有業務規則）"]
    end

    REST -->|呼叫| HANDLER
    HANDLER -->|使用| AGG
    HANDLER -->|依賴| PORTS
    DB -.實作.-> PORTS
    DB -->|回傳| AGG

    style Domain fill:#e8f5e9,stroke:#2e7d32
    style Application fill:#e3f2fd,stroke:#1565c0
    style Infrastructure fill:#fff3e0,stroke:#e65100
```

- **Domain** 不依賴任何東西——沒有 Spring、沒有 JPA、沒有任何 annotation。它是純 Java。這就是「純粹」的意思。
- **Application** 只依賴 Domain，以及它自己定義的介面（也就是 **Port**）。
- **Infrastructure** 依賴所有層——它是把應用程式接到真實世界的黏著劑。

讓新手意外的那支箭頭：資料庫 Adapter（`DB`）往*上*指向 Application 的 `PORTS`（虛線「實作」箭頭）。Application 透過宣告一個介面
說「我需要一個能載入帳戶的東西」；Infrastructure 提供實作。這就是**依賴反轉（Dependency Inversion）**，也正是為什麼核心永遠
不必知道資料是來自 PostgreSQL、Redis 還是一個 `HashMap`。

---

## 3. 三個層次

對應到 `src/main/java/com/bank/accountquery/` 底下的資料夾：

| 層次 | 資料夾 | 職責 | 可依賴 |
|------|--------|------|--------|
| **Domain** | `domain/` | 業務規則與概念（Account、Money…） | 無（純 Java） |
| **Application** | `application/` | 協調一個 Use Case；定義 Ports | 只有 Domain |
| **Infrastructure** | `infrastructure/` | HTTP、持久化、框架接線 | Application + Domain + Spring |

```
domain/
├── model/
│   ├── shared/      Money、Currency、DateRange、CustomerId
│   ├── account/     Account（aggregate）、Transaction、TransactionHistory…
│   └── privilege/   TransferPrivilege（aggregate）、PrivilegeUsageRecord…
└── exception/       AccountNotOwnedByCustomerException、QueryRangeExceededException…

application/
├── port/
│   ├── in/          Use Case 介面（這個應用程式能做什麼）
│   └── out/         Load*Port 介面（這個應用程式需要什麼）← Repository 介面放在這裡
└── query/
    ├── account/     Query 物件 + Handlers + 結果 DTO
    ├── privilege/   Query 物件 + Handlers + 結果 DTO
    └── common/      PageInfo、Pagination

infrastructure/
├── adapter/
│   ├── in/rest/     Controllers、GlobalExceptionHandler、ApiResponse、CurrentCustomer
│   └── out/persistence/inmemory/   實作 Load*Port 的 Adapters
└── config/          WebConfig
```

---

## 4. 架構總覽（圖）

「六角形」：應用核心在中央，左邊是**驅動端（driving）** Adapter（呼叫我們的東西），右邊是**被驅動端（driven）**
Adapter（我們呼叫的東西）。

```mermaid
flowchart LR
    Client(["HTTP 用戶端<br/>curl / app"])

    subgraph Core["應用核心 Application Core"]
        direction TB
        InPort["«input port»<br/>GetTwdTransactionHistoryUseCase"]
        Handler["GetTwdTransactionHistoryHandler"]
        OutPort1["«output port»<br/>LoadAccountPort"]
        OutPort2["«output port»<br/>LoadTransactionPort"]
        Domain["«domain»<br/>Account aggregate<br/>（業務規則）"]
        InPort --> Handler
        Handler --> Domain
        Handler --> OutPort1
        Handler --> OutPort2
    end

    Controller["AccountController<br/>（driving adapter）"]
    AccAdapter["InMemoryAccountAdapter<br/>（driven adapter）"]
    TxAdapter["InMemoryTransactionAdapter<br/>（driven adapter）"]
    Store[("InMemoryBankingDataStore")]

    Client -->|GET /transactions/twd| Controller
    Controller -->|建立 Query 並呼叫| InPort
    OutPort1 -. 由其實作 .-> AccAdapter
    OutPort2 -. 由其實作 .-> TxAdapter
    AccAdapter --> Store
    TxAdapter --> Store

    style Core fill:#e3f2fd,stroke:#1565c0
    style Domain fill:#e8f5e9,stroke:#2e7d32
```

把 `InMemory*Adapter` 換成 `*JpaAdapter`，**核心完全不需要改動**——這就是「對 Port 寫程式」帶來的回報。

---

## 5. 類別圖 — 領域模型

兩個 **aggregate**（`Account`、`TransferPrivilege`）是整個系統的核心。aggregate 是一群被當作單一單位看待的物件，由單一的
**root** 守護所有規則。請注意規則是*以模型上的方法*存在（`verifyOwnership`、`ensureActive`、`filterByDateRange`、
`isValid`），而不是放在某個「service」類別裡。

```mermaid
classDiagram
    direction TB

    class Account {
        -AccountId accountId
        -CustomerId ownerId
        -AccountType accountType
        -Currency currency
        -AccountStatus status
        +verifyOwnership(CustomerId) void
        +ensureActive() void
        +filterByDateRange(List~Transaction~, DateRange) TransactionHistory
        +isOwnedBy(CustomerId) boolean
    }
    class Transaction {
        -TransactionId id
        -TransactionType type
        -Money amount
        -Money twdEquivalent
        -LocalDateTime transactionDate
        +exchangeRate() BigDecimal
    }
    class TransactionHistory {
        <<value object>>
        +AccountId accountId
        +List~Transaction~ transactions
        +DateRange queriedRange
        +count() int
    }

    class TransferPrivilege {
        -PrivilegeId privilegeId
        -CustomerId ownerId
        -PrivilegeType type
        -int totalQuota
        -int usedQuota
        -DateRange validPeriod
        +isValid() boolean
        +isExpired() boolean
        +getRemainingQuota() int
        +filterUsageHistory(DateRange) PrivilegeUsageHistory
        +verifyOwnership(CustomerId) void
    }
    class PrivilegeUsageRecord {
        <<value object>>
        +LocalDate usedDate
        +Money savedAmount
        +String targetAccountNo
    }

    class Money {
        <<value object>>
        +BigDecimal amount
        +Currency currency
        +add(Money) Money
    }
    class DateRange {
        <<value object>>
        +LocalDate startDate
        +LocalDate endDate
        +exceedsMonths(int) boolean
        +contains(LocalDate) boolean
    }
    class CustomerId {
        <<value object>>
        +String value
    }

    Account "1" *-- "many" Transaction : contains
    Account ..> TransactionHistory : produces
    Transaction *-- Money : amount
    Account --> CustomerId : owned by
    TransferPrivilege "1" *-- "many" PrivilegeUsageRecord : contains
    TransferPrivilege --> CustomerId : owned by
    TransferPrivilege ..> DateRange : validPeriod
    PrivilegeUsageRecord *-- Money : savedAmount
```

**為什麼 `Money`、`DateRange`、`CustomerId` 要自成型別**（而不是直接用 `BigDecimal`、兩個 `LocalDate`、一個 `String`）：
因為它們攜帶規則。`Money` 拒絕負數金額，也不會把兩種不同幣別相加。`DateRange` 拒絕起日晚於迄日，並能回答
`exceedsMonths(13)`。這是治療*基本型別偏執（Primitive Obsession）*的解藥——bug 在建構當下、處處、免費地被攔下來。

---

## 6. 循序圖 — 一次請求發生了什麼

`GET /api/v1/accounts/{id}/transactions/twd`。注意各個職責落在哪裡：**Controller** 只做 HTTP 轉換，**Handler** 只做協調，
而每一個業務決策都由 **Account** 做出。

```mermaid
sequenceDiagram
    autonumber
    actor Client as 用戶端
    participant C as AccountController<br/>(driving adapter)
    participant UC as GetTwdTransactionHistoryHandler<br/>(use case)
    participant AP as LoadAccountPort
    participant A as Account<br/>(aggregate)
    participant TP as LoadTransactionPort
    participant R as TwdTransactionHistoryResult

    Client->>C: GET /transactions/twd + X-Customer-Id
    C->>C: 建立 CustomerId、AccountId、DateRange<br/>（參數不合法 → 此處 400）
    C->>UC: execute(query)

    UC->>AP: findByAccountId(accountId)
    AP-->>UC: Account（找不到 → 404）

    UC->>A: verifyOwnership(customerId)
    Note right of A: 非持有人 →<br/>拋例外 → 403
    UC->>A: ensureActive()
    Note right of A: 凍結/結清 →<br/>拋例外 → 422

    UC->>TP: findByAccountId(accountId, dateRange)
    TP-->>UC: 原始 List~Transaction~

    UC->>A: filterByDateRange(transactions, dateRange)
    Note right of A: 區間 > 13 個月 →<br/>拋例外 → 422
    A-->>UC: TransactionHistory（已過濾）

    UC->>R: from(history, page, size)
    R-->>UC: 結果 DTO
    UC-->>C: result
    C-->>Client: 200 { code: SUCCESS, data: … }
```

被拋出的例外不會讓 Controller 充滿 `if/else`。它們往上冒泡到單一的 `GlobalExceptionHandler`，由它把每一種例外轉換成正確的
HTTP 狀態碼——見[§10](#10-錯誤如何轉換成-http-狀態碼)。

---

## 7. ER 圖 — 資料如何關聯

本服務為唯讀，且內建記憶體資料，但其領域模型能乾淨地對應到 Tutorial 針對 PostgreSQL 所規劃的關聯式 schema。概念上：

```mermaid
erDiagram
    CUSTOMER ||--o{ ACCOUNT : owns
    CUSTOMER ||--o{ TRANSFER_PRIVILEGE : owns
    ACCOUNT ||--o{ TRANSACTION : has
    TRANSFER_PRIVILEGE ||--o{ PRIVILEGE_USAGE_RECORD : has

    CUSTOMER {
        string customer_id PK
    }
    ACCOUNT {
        string account_id PK "14 碼數字"
        string owner_id FK
        string account_type "TWD | FX"
        string currency "TWD | USD | JPY | EUR"
        string status "ACTIVE | FROZEN | CLOSED"
    }
    TRANSACTION {
        string transaction_id PK
        string account_id FK
        string type "CREDIT | DEBIT"
        decimal amount
        string amount_currency
        decimal twd_equivalent "可為空，僅外幣"
        datetime transaction_date
        string description
        string channel
    }
    TRANSFER_PRIVILEGE {
        string privilege_id PK
        string owner_id FK
        string type
        int total_quota
        int used_quota
        date valid_from
        date valid_to
    }
    PRIVILEGE_USAGE_RECORD {
        bigint id PK
        string privilege_id FK
        date used_date
        decimal saved_amount
        string target_account_no
    }
```

每個方框都與一個領域型別一對一對應（`ACCOUNT` ↔ `Account`、`TRANSACTION` ↔ `Transaction` 等）。從 `ACCOUNT` 與
`TRANSFER_PRIVILEGE` 出發的那兩條「`||--o{`」關係，就是 **aggregate 邊界**：你永遠是*透過* `Account` 來載入/儲存
`TRANSACTION`，而不會單獨操作它（見 Tutorial 的 ADR-001）。

---

## 8. 設計決策說明

**業務規則屬於模型。** 所有權檢查、「這個帳戶是否啟用？」、「區間是否 ≤ 13 個月？」、「這個優惠是否仍有效？」都是
`Account` / `TransferPrivilege` 上的方法。Handler 刻意保持無聊——它只負責取得、委派、轉換。如果你在 Handler 裡看到
`if (account.getStatus() == FROZEN)`，那就是一個壞味道：規則從模型裡漏出去了。

**Repository 介面放在 Application 層，並以意圖命名。** 它們叫做 `LoadAccountPort`、`LoadTransactionPort`——是*描述應用程式
需要什麼的動詞*，而不是 `AccountRepository`（那會暗示資料庫）。Application 宣告需求；Infrastructure 滿足它。這讓 Domain
完全不沾持久化的概念（Domain 甚至不定義這些介面）。

**為了效能，讀取被拆開（CQRS 讀取側）。** 一個繁忙的帳戶可能有上萬筆交易。只為了讀帳戶狀態就把它們全部載入是浪費，因此
`LoadAccountPort` 回傳*不含*交易的帳戶，再由 `LoadTransactionPort` 單獨抓取有日期界限的片段。接著由 `Account` 套用最終的業務
過濾。這就是 Tutorial ADR-002 的「方案 A」——在把規則留在領域層的同時，仍保持快速。

**處處不可變（Immutability）。** Value Object 與結果 DTO 都是 Java `record`；`TransactionHistory` 對它的清單做防禦性複製。
不可變物件在建立之後無法被破壞，這消除了一整類 bug，也讓程式碼在本應用啟用的虛擬執行緒併發下保持安全。

**錯誤翻譯只有一個地方。** `GlobalExceptionHandler`（一個 Spring `@RestControllerAdvice`）是*唯一*知道業務錯誤要對應到哪個
HTTP 狀態碼的地方。Domain 例外保持與框架無關；對應關係集中在一張好讀的方法表裡。

**本教學切片的認證是模擬的。** 這裡不用完整的 Spring Security + JWT，而是用一個自訂的 `@CurrentCustomer` 參數解析器讀取
`X-Customer-Id` Header 並轉成 `CustomerId`。這保留了重要的邊界——*Controller 不做業務授權*（授權由領域的 `verifyOwnership`
完成）——同時讓專案可執行、好測試。正式版只要把解析器換成讀取 JWT principal 的版本即可，其他都不用動。

---

## 9. API

| 方法 | 路徑 | 說明 |
|------|------|------|
| `GET` | `/api/v1/accounts/{accountId}/transactions/twd` | 台幣交易紀錄 |
| `GET` | `/api/v1/accounts/{accountId}/transactions/fx` | 外幣交易紀錄（呈現台幣等值 + 匯率） |
| `GET` | `/api/v1/customers/me/privileges/transfer` | 列出轉帳優惠 |
| `GET` | `/api/v1/customers/me/privileges/transfer/{privilegeId}/usage` | 某項優惠的使用紀錄 |

共用查詢參數：`startDate`、`endDate`（`YYYY-MM-DD`）、`page`（預設 0）、`size`（預設 20，上限 100）。外幣端點另需
`currency`（例如 `USD`）。所有請求都需要 `X-Customer-Id` Header。

每個回應都使用統一的外層格式：

```json
// 成功
{ "code": "SUCCESS", "data": { "...": "..." }, "timestamp": "2026-06-28T21:09:44+08:00" }

// 失敗
{ "code": "ACCOUNT_NOT_OWNED_BY_CUSTOMER", "message": "…", "timestamp": "…" }
```

---

## 10. 錯誤如何轉換成 HTTP 狀態碼

流程永遠是：**領域拋出有業務語意的例外 → `GlobalExceptionHandler` 對應 → 用戶端收到狀態碼 + code。**

| 例外（由誰拋出） | HTTP | `code` |
|------------------|------|--------|
| `InvalidAccountIdFormatException` / 參數錯誤（Controller） | 400 | `INVALID_ACCOUNT_ID` / `BAD_REQUEST` |
| `UnsupportedCurrencyException`（Currency） | 400 | `UNSUPPORTED_CURRENCY` |
| 缺少 `X-Customer-Id`（解析器） | 401 | `UNAUTHORIZED` |
| `AccountNotOwnedByCustomerException`（Account） | 403 | `ACCOUNT_NOT_OWNED_BY_CUSTOMER` |
| `PrivilegeNotOwnedByCustomerException`（TransferPrivilege） | 403 | `PRIVILEGE_NOT_OWNED_BY_CUSTOMER` |
| `AccountNotFoundException`（Handler） | 404 | `ACCOUNT_NOT_FOUND` |
| `PrivilegeNotFoundException`（Handler） | 404 | `PRIVILEGE_NOT_FOUND` |
| `AccountNotActiveException`（Account） | 422 | `ACCOUNT_NOT_ACTIVE` |
| `QueryRangeExceededException`（Account） | 422 | `QUERY_RANGE_EXCEEDED` |
| `AccountCurrencyMismatchException`（Account） | 422 | `ACCOUNT_CURRENCY_MISMATCH` |

---

## 11. 測試策略

測試對應各層，並以最快者優先執行——這就是實務上的 TDD 金字塔（共 33 個測試）：

```mermaid
flowchart TB
    D["Domain 單元測試<br/>純 JUnit，不用 Spring<br/>MoneyTest、DateRangeTest、AccountTest、TransferPrivilegeTest"]
    A["Application 測試<br/>JUnit + Mockito<br/>mock 掉 Load*Ports，驗證協調流程"]
    W["Controller 測試<br/>@WebMvcTest + MockMvc<br/>驗證每條路徑的 HTTP 狀態與 JSON"]
    D --> A --> W
    style D fill:#e8f5e9,stroke:#2e7d32
    style A fill:#e3f2fd,stroke:#1565c0
    style W fill:#fff3e0,stroke:#e65100
```

- **Domain 測試**純粹且即時——它們證明規則（例如「把 USD 加到 TWD 會拋例外」、「14 個月的區間會被拒絕」）。
- **Application 測試**把 Port mock 掉（`@Mock LoadAccountPort`），因此*只*測 Handler 的協調流程——例如「找不到帳戶時，
  我們絕不查詢交易」。
- **Controller 測試**把 Use Case mock 掉，並斷言 HTTP 合約——狀態碼與 JSON 外層格式。

只跑某一層，例如：`./gradlew test --tests "*AccountTest"`。

---

## 12. 與 Tutorial 的差異

為了讓這個切片在任何只裝了 JDK 的機器上都能執行，本實作用較輕量、但**介面相同**的等價物取代了笨重的基礎設施：

| Tutorial | 本實作 | 為何安全 |
|----------|--------|----------|
| Java 23 | Java 25 | 超集；使用的語言特性完全相同 |
| PostgreSQL + JPA + Redis + Testcontainers | 記憶體 Adapter | 它們實作相同的 `Load*Port`；換成 `*JpaAdapter` 不需改核心 |
| Spring Security + JWT | `X-Customer-Id` Header + 參數解析器 | 維持「Controller 不做授權」的邊界；可輕易替換 |
| Cucumber BDD、WireMock、Micrometer、OpenAPI | 以 `@WebMvcTest` + curl 覆蓋 | 屬 Sprint-5 範圍，不在此切片內 |

完整的設計理由（包含三個被否決的讀取側方案與各項 ADR）收錄於 [`banking-api-tutorial-v2.md`](banking-api-tutorial-v2.md)。

---

## 名詞解釋

- **Domain（領域）** — 用程式碼建模的業務世界（帳戶、金錢、優惠），不含任何技術考量。
- **Aggregate / Aggregate Root（聚合／聚合根）** — 一群相關物件被當作單一單位；其*root*（例如 `Account`）是唯一入口，
  並強制執行整群的規則。這裡的 `Transaction` 只能透過它的 `Account` 取得。
- **Entity（實體）** — 具有身分、會隨時間延續的物件（例如以 `TransactionId` 識別的 `Transaction`）。
- **Value Object（值物件）** — 純粹由其值定義、不可變、沒有身分的物件（例如 `Money`、`DateRange`）。兩個
  `Money(100, TWD)` 可互換。
- **Port（埠）** — 由應用核心擁有的介面。*Input port* 是應用程式對外提供的 Use Case；*output port*（`Load*Port`）是應用程式
  需要外界提供的能力。
- **Adapter（轉接器）** — 位於 Infrastructure、實作某個 Port 的具體類別。*Driving*（驅動端）adapter 呼叫應用程式（REST
  controller）；*driven*（被驅動端）adapter 被應用程式呼叫（持久化）。
- **CQRS** — Command Query Responsibility Segregation（命令查詢職責分離）：把寫入路徑與讀取路徑分開。本專案只實作讀取
  （查詢）側。
- **Dependency Inversion（依賴反轉）** — 高階程式碼依賴介面而非具體細節；細節反過來依賴介面。正是這點讓資料庫能往「內」
  指向應用程式的 Port。
- **DTO** — Data Transfer Object（資料傳輸物件）：用來跨邊界搬運資料的單純結構（這裡是回傳給用戶端的 JSON `*Result` /
  `*Dto` record）。
