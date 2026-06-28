package com.bank.accountquery.infrastructure.bootstrap;

import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.AccountEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeUsageRecordEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.TransactionEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.AccountJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.PrivilegeJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.TransactionJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 啟動時寫入示範資料（與舊版記憶體種子相同），方便本機 curl 試用。
 * 由 app.demo-seed.enabled 控制；整合測試會關閉，改由情境自行佈置資料。
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final boolean enabled;
    private final AccountJpaRepository accountRepository;
    private final TransactionJpaRepository transactionRepository;
    private final PrivilegeJpaRepository privilegeRepository;

    public DemoDataSeeder(@Value("${app.demo-seed.enabled:true}") boolean enabled,
                          AccountJpaRepository accountRepository,
                          TransactionJpaRepository transactionRepository,
                          PrivilegeJpaRepository privilegeRepository) {
        this.enabled = enabled;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || accountRepository.count() > 0) {
            return;
        }

        accountRepository.saveAll(List.of(
            new AccountEntity("00123456789012", "C001", "TWD", "TWD", "ACTIVE"),
            new AccountEntity("00123456789013", "C001", "FX", "USD", "ACTIVE"),
            new AccountEntity("00999999999999", "C002", "TWD", "TWD", "ACTIVE")));

        transactionRepository.saveAll(List.of(
            new TransactionEntity("T001", "00123456789012", "CREDIT", new BigDecimal("50000.00"), "TWD",
                null, LocalDateTime.of(2025, 1, 5, 9, 0), "薪資轉帳", "SYSTEM"),
            new TransactionEntity("T002", "00123456789012", "DEBIT", new BigDecimal("10000.00"), "TWD",
                null, LocalDateTime.of(2025, 1, 10, 14, 30), "ATM 提款", "ATM"),
            new TransactionEntity("T003", "00123456789012", "CREDIT", new BigDecimal("3000.00"), "TWD",
                null, LocalDateTime.of(2025, 1, 20, 0, 0), "利息入帳", "SYSTEM"),
            new TransactionEntity("F001", "00123456789013", "CREDIT", new BigDecimal("1000.00"), "USD",
                new BigDecimal("32500.00"), LocalDateTime.of(2025, 1, 5, 10, 0), "美元購匯", "ONLINE")));

        var p001 = new PrivilegeEntity("P001", "C001", "FEE_FREE_INTERBANK_TRANSFER",
            10, 3, LocalDate.of(2025, 1, 1), LocalDate.of(2030, 12, 31));
        p001.replaceUsageRecords(List.of(
            new PrivilegeUsageRecordEntity(LocalDate.of(2025, 1, 5), new BigDecimal("30"), "81234567890123"),
            new PrivilegeUsageRecordEntity(LocalDate.of(2025, 1, 12), new BigDecimal("30"), "91111222333444")));

        var p002 = new PrivilegeEntity("P002", "C001", "FEE_FREE_INTERBANK_TRANSFER",
            5, 2, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        var p999 = new PrivilegeEntity("P999", "C002", "FEE_FREE_WIRE_TRANSFER",
            5, 0, LocalDate.of(2025, 1, 1), LocalDate.of(2030, 12, 31));

        privilegeRepository.saveAll(List.of(p001, p002, p999));
    }
}
