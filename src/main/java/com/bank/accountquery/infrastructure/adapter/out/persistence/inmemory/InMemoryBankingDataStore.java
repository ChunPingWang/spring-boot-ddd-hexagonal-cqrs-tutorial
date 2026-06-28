package com.bank.accountquery.infrastructure.adapter.out.persistence.inmemory;

import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.account.AccountStatus;
import com.bank.accountquery.domain.model.account.AccountType;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.account.TransactionChannel;
import com.bank.accountquery.domain.model.account.TransactionId;
import com.bank.accountquery.domain.model.account.TransactionType;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.privilege.PrivilegeUsageRecord;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 內建記憶體資料源（demo / 本機驗證用），取代真實 PostgreSQL / Redis。
 * 正式版以 JPA Adapter（AccountJpaAdapter 等）取代，介面契約相同（LSP）。
 */
@Component
public class InMemoryBankingDataStore {

    private final Map<AccountId, Account> accounts = new LinkedHashMap<>();
    private final Map<AccountId, List<Transaction>> transactions = new LinkedHashMap<>();
    private final Map<PrivilegeId, TransferPrivilege> privileges = new LinkedHashMap<>();

    public InMemoryBankingDataStore() {
        seed();
    }

    private void seed() {
        var c001 = CustomerId.of("C001");
        var c002 = CustomerId.of("C002");

        // ── 帳戶 ──────────────────────────────────────────────────
        var twdAccountId = new AccountId("00123456789012");
        var fxAccountId = new AccountId("00123456789013");
        var otherAccountId = new AccountId("00999999999999");

        accounts.put(twdAccountId,
            new Account(twdAccountId, c001, AccountType.TWD, Currency.TWD, AccountStatus.ACTIVE));
        accounts.put(fxAccountId,
            new Account(fxAccountId, c001, AccountType.FX, Currency.USD, AccountStatus.ACTIVE));
        accounts.put(otherAccountId,
            new Account(otherAccountId, c002, AccountType.TWD, Currency.TWD, AccountStatus.ACTIVE));

        // ── 台幣交易（2025-01）────────────────────────────────────
        transactions.put(twdAccountId, List.of(
            twdTx("T001", TransactionType.CREDIT, "50000.00", LocalDateTime.of(2025, 1, 5, 9, 0), "薪資轉帳", TransactionChannel.SYSTEM),
            twdTx("T002", TransactionType.DEBIT, "10000.00", LocalDateTime.of(2025, 1, 10, 14, 30), "ATM 提款", TransactionChannel.ATM),
            twdTx("T003", TransactionType.CREDIT, "3000.00", LocalDateTime.of(2025, 1, 20, 0, 0), "利息入帳", TransactionChannel.SYSTEM)
        ));

        // ── 外幣交易（USD，2025-01）────────────────────────────────
        transactions.put(fxAccountId, List.of(
            new Transaction(
                TransactionId.of("F001"), TransactionType.CREDIT,
                Money.of(new BigDecimal("1000.00"), Currency.USD),
                Money.twd(new BigDecimal("32500.00")),
                LocalDateTime.of(2025, 1, 5, 10, 0), "美元購匯", TransactionChannel.ONLINE)
        ));

        // ── 轉帳優惠 ──────────────────────────────────────────────
        privileges.put(new PrivilegeId("P001"), new TransferPrivilege(
            new PrivilegeId("P001"), c001, PrivilegeType.FEE_FREE_INTERBANK_TRANSFER,
            10, 3, new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 12, 31)),
            List.of(
                new PrivilegeUsageRecord(LocalDate.of(2025, 1, 5), Money.twd(new BigDecimal("30")), "81234567890123"),
                new PrivilegeUsageRecord(LocalDate.of(2025, 1, 12), Money.twd(new BigDecimal("30")), "91111222333444")
            )
        ));
        privileges.put(new PrivilegeId("P002"), new TransferPrivilege(
            new PrivilegeId("P002"), c001, PrivilegeType.FEE_FREE_INTERBANK_TRANSFER,
            5, 2, new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
            List.of()
        ));
        privileges.put(new PrivilegeId("P999"), new TransferPrivilege(
            new PrivilegeId("P999"), c002, PrivilegeType.FEE_FREE_WIRE_TRANSFER,
            5, 0, new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 12, 31)),
            List.of()
        ));
    }

    private static Transaction twdTx(String id, TransactionType type, String amount,
                                     LocalDateTime when, String desc, TransactionChannel channel) {
        return new Transaction(
            TransactionId.of(id), type, Money.twd(new BigDecimal(amount)), null, when, desc, channel);
    }

    Map<AccountId, Account> accounts()                  { return accounts; }
    Map<AccountId, List<Transaction>> transactions()    { return transactions; }
    Map<PrivilegeId, TransferPrivilege> privileges()    { return privileges; }

    // ── 寫入操作（供 BDD 情境測試於每個情境前自行佈置資料；亦可供未來 Command 側使用）──
    public void reset() {
        accounts.clear();
        transactions.clear();
        privileges.clear();
    }

    public void addAccount(Account account) {
        accounts.put(account.getAccountId(), account);
    }

    public void putTransactions(AccountId accountId, List<Transaction> txs) {
        transactions.put(accountId, List.copyOf(txs));
    }

    public void addPrivilege(TransferPrivilege privilege) {
        privileges.put(privilege.getPrivilegeId(), privilege);
    }

    /** 為既有優惠附加使用紀錄（TransferPrivilege 不可變，故以相同欄位重建）。 */
    public void replaceUsageRecords(PrivilegeId privilegeId, List<PrivilegeUsageRecord> records) {
        TransferPrivilege p = privileges.get(privilegeId);
        if (p == null) {
            throw new IllegalStateException("優惠不存在，無法附加使用紀錄：" + privilegeId.value());
        }
        privileges.put(privilegeId, new TransferPrivilege(
            p.getPrivilegeId(), p.getOwnerId(), p.getType(),
            p.getTotalQuota(), p.getUsedQuota(), p.getValidPeriod(), records));
    }
}
