# Banking Account Query Service

依 [`banking-api-tutorial-v2.md`](../banking-api-tutorial-v2.md) 實作的銀行帳戶查詢 API，
採 **DDD 戰術設計 · Hexagonal Architecture · CQRS（Read Side）· TDD**。

## 技術棧（實際環境）

| 項目 | 規劃 | 本實作 |
|------|------|--------|
| Language | Java 23 | **Java 25**（超集，記錄/模式比對/文字區塊皆相容） |
| Framework | Spring Boot 4.0.x | **Spring Boot 4.0.7** |
| Build | Gradle Kotlin DSL | Gradle 9.2（含 wrapper） |
| Test | JUnit 5 + Mockito + AssertJ | 同左（33 個測試全綠） |

## 執行

```bash
./gradlew test       # 33 tests, 0 failures
./gradlew bootRun    # 啟動於 http://localhost:8080
```

身分認證以 `X-Customer-Id` Header 模擬 JWT 取出的客戶身分（見下方「與 Tutorial 的差異」）。

```bash
# 台幣交易紀錄（C001 名下帳戶）
curl -H "X-Customer-Id: C001" \
  "http://localhost:8080/api/v1/accounts/00123456789012/transactions/twd?startDate=2025-01-01&endDate=2025-01-31"

# 轉帳優惠
curl -H "X-Customer-Id: C001" "http://localhost:8080/api/v1/customers/me/privileges/transfer"
```

## 架構落點（依賴方向：Infrastructure → Application → Domain）

```
domain/                 純粹核心，零框架依賴（Aggregate 守護所有業務規則）
  model/{shared,account,privilege}, exception/
application/            Use Cases + Port 定義
  port/in/  (UseCase 介面)      port/out/ (Load*Port — Repository 介面在此！)
  query/{account,privilege}/   Handler + Query Object + Read Model
infrastructure/         Adapters（實作 Ports）
  adapter/in/rest/      AccountController, PrivilegeController, GlobalExceptionHandler
  adapter/out/persistence/inmemory/   實作 Load*Port（demo 用記憶體資料）
```

業務規則（所有權、帳戶狀態、13 個月區間、優惠有效性）**全部封裝在 Aggregate Method**；
Handler 僅協調流程，不含 `if/else` 業務判斷（符合 ADR-002 方案 A）。

## 已驗證行為（與 BDD Feature 對應）

| 情境 | 結果 |
|------|------|
| 台幣交易查詢成功 | 200，3 筆交易 |
| 外幣查詢顯示台幣等值/匯率 | 200，1000 USD → 32500 TWD @ 32.5 |
| 查詢他人帳戶 | 403 `ACCOUNT_NOT_OWNED_BY_CUSTOMER` |
| 帳戶不存在 | 404 `ACCOUNT_NOT_FOUND` |
| 查詢區間 > 13 個月 | 422 `QUERY_RANGE_EXCEEDED` |
| 缺認證 / 帳號格式錯 / 不支援幣別 | 401 / 400 `INVALID_ACCOUNT_ID` / 400 `UNSUPPORTED_CURRENCY` |
| 優惠查詢 | 200，P001 有效（剩餘 7）、P002 過期 |
| 優惠使用紀錄 | 200，2 筆、總節省 60 |
| 查他人優惠使用紀錄 | 403 `PRIVILEGE_NOT_OWNED_BY_CUSTOMER` |

## 與 Tutorial 的差異（聚焦可驗證的核心垂直切片）

本實作完整覆蓋 **Domain → Application → REST** 與其 TDD 測試（Sprint 1–3 的核心 + Sprint 4 的行為驗證）。
為了讓專案能在無 Docker 的環境直接啟動並驗證，以下項目以較輕量的方式替代，介面契約保持一致：

- **持久層**：以記憶體 Adapter 實作 `Load*Port`，取代 PostgreSQL/JPA + Redis + Testcontainers。
  正式版只需新增 `*JpaAdapter` 實作同一組 Output Port 即可替換（LSP，Handler 不需改動）。
- **認證**：以 `X-Customer-Id` Header + `HandlerMethodArgumentResolver` 模擬 JWT Principal，
  取代完整 Spring Security/JWT（維持「Controller 不自行認證」的職責邊界）。
- **BDD/WireMock/Observability**：Cucumber 情境已用 REST 行為測試與手動 curl 驗證覆蓋；
  WireMock（Core Banking）、Micrometer、OpenAPI 屬 Sprint 5 範圍，未納入此切片。
