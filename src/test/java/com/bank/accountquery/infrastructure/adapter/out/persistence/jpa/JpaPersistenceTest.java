package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.accountquery.application.port.out.LoadAccountPort;
import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.port.out.LoadTransactionPort;
import com.bank.accountquery.application.port.out.SavePrivilegePort;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.AccountEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.TransactionEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.AccountJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.PrivilegeJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.TransactionJpaRepository;
import com.bank.accountquery.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 整合測試：在真實 PostgreSQL（Testcontainers）上驗證 JPA Adapter 與 Aggregate 持久化。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "app.demo-seed.enabled=false")
@Import(TestcontainersConfiguration.class)
class JpaPersistenceTest {

    @Autowired LoadAccountPort loadAccountPort;
    @Autowired LoadTransactionPort loadTransactionPort;
    @Autowired LoadPrivilegePort loadPrivilegePort;
    @Autowired SavePrivilegePort savePrivilegePort;
    @Autowired AccountJpaRepository accountRepository;
    @Autowired TransactionJpaRepository transactionRepository;
    @Autowired PrivilegeJpaRepository privilegeRepository;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        privilegeRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("帳戶與交易能寫入 PostgreSQL 並由 Port 正確映射回 Domain")
    void account_and_transaction_round_trip() {
        accountRepository.save(new AccountEntity("00123456789012", "C001", "TWD", "TWD", "ACTIVE"));
        transactionRepository.save(new TransactionEntity("T1", "00123456789012", "CREDIT",
            new BigDecimal("100.00"), "TWD", null, LocalDateTime.of(2025, 1, 10, 9, 0), "測試", "SYSTEM"));

        var account = loadAccountPort.findByAccountId(new AccountId("00123456789012")).orElseThrow();
        assertThat(account.getOwnerId()).isEqualTo(CustomerId.of("C001"));
        assertThat(account.isOwnedBy(CustomerId.of("C001"))).isTrue();

        var txs = loadTransactionPort.findByAccountId(new AccountId("00123456789012"),
            new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getAmount().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("以 Aggregate Root 為單位儲存：use() 後使用次數與使用紀錄都被持久化")
    void privilege_aggregate_save_and_use_round_trip() {
        var privilege = new TransferPrivilege(
            PrivilegeId.of("P001"), CustomerId.of("C001"), PrivilegeType.FEE_FREE_INTERBANK_TRANSFER,
            10, 0, new DateRange(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1)), List.of());
        savePrivilegePort.save(privilege);

        // 重新載入 → 使用一次 → 再存（模擬 Command Handler 流程）
        var loaded = loadPrivilegePort.findByPrivilegeId(PrivilegeId.of("P001")).orElseThrow();
        loaded.use(CustomerId.of("C001"), Money.twd(new BigDecimal("15")), "81234567890123");
        savePrivilegePort.save(loaded);

        var reloaded = loadPrivilegePort.findByPrivilegeId(PrivilegeId.of("P001")).orElseThrow();
        assertThat(reloaded.getUsedQuota()).isEqualTo(1);
        assertThat(reloaded.getRemainingQuota()).isEqualTo(9);

        var range = new DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        assertThat(reloaded.filterUsageHistory(range).count()).isEqualTo(1);
    }
}
