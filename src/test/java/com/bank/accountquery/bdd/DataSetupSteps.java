package com.bank.accountquery.bdd;

import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.AccountEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeUsageRecordEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.TransactionEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.AccountJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.PrivilegeJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.TransactionJpaRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Given 步驟：在真實 PostgreSQL 中佈置每個情境所需的帳戶、交易、優惠與認證身分。
 */
public class DataSetupSteps {

    private final AccountJpaRepository accountRepository;
    private final TransactionJpaRepository transactionRepository;
    private final PrivilegeJpaRepository privilegeRepository;
    private final ScenarioContext context;

    public DataSetupSteps(AccountJpaRepository accountRepository,
                          TransactionJpaRepository transactionRepository,
                          PrivilegeJpaRepository privilegeRepository,
                          ScenarioContext context) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.privilegeRepository = privilegeRepository;
        this.context = context;
    }

    @Given("客戶 {string} 已完成身份認證")
    public void customerAuthenticated(String customerId) {
        context.customerId = customerId;
    }

    @Given("客戶 {string} 有一個啟用的台幣帳戶 {string}")
    public void activeTwdAccount(String customerId, String accountId) {
        accountRepository.save(new AccountEntity(accountId, customerId, "TWD", "TWD", "ACTIVE"));
    }

    @Given("客戶 {string} 有一個凍結的台幣帳戶 {string}")
    public void frozenTwdAccount(String customerId, String accountId) {
        accountRepository.save(new AccountEntity(accountId, customerId, "TWD", "TWD", "FROZEN"));
    }

    @Given("客戶 {string} 有一個啟用的美元帳戶 {string}")
    public void activeUsdAccount(String customerId, String accountId) {
        accountRepository.save(new AccountEntity(accountId, customerId, "FX", "USD", "ACTIVE"));
    }

    @Given("帳戶 {string} 有以下交易:")
    public void twdTransactions(String accountId, DataTable table) {
        var rows = table.asMaps();
        var list = new ArrayList<TransactionEntity>();
        int i = 0;
        for (Map<String, String> row : rows) {
            list.add(new TransactionEntity(
                accountId + "-T" + (i++),
                accountId,
                "存入".equals(row.get("類型")) ? "CREDIT" : "DEBIT",
                new BigDecimal(row.get("金額")),
                "TWD",
                null,
                LocalDate.parse(row.get("日期")).atStartOfDay(),
                row.get("說明"),
                "SYSTEM"));
        }
        transactionRepository.saveAll(list);
    }

    @Given("帳戶 {string} 有以下美元交易:")
    public void fxTransactions(String accountId, DataTable table) {
        var rows = table.asMaps();
        var list = new ArrayList<TransactionEntity>();
        int i = 0;
        for (Map<String, String> row : rows) {
            var fx = new BigDecimal(row.get("原幣金額"));
            var rate = new BigDecimal(row.get("匯率"));
            list.add(new TransactionEntity(
                accountId + "-F" + (i++),
                accountId,
                "存入".equals(row.get("類型")) ? "CREDIT" : "DEBIT",
                fx,
                "USD",
                fx.multiply(rate).setScale(2, RoundingMode.HALF_UP),
                LocalDate.parse(row.get("日期")).atStartOfDay(),
                row.get("說明"),
                "ONLINE"));
        }
        transactionRepository.saveAll(list);
    }

    @Given("客戶 {string} 有一項優惠 {string} 類型 {string} 總次數 {int} 已用 {int} 有效期 {string} 至 {string}")
    public void privilege(String customerId, String privilegeId, String typeDesc,
                          int total, int used, String validFrom, String validTo) {
        privilegeRepository.save(new PrivilegeEntity(
            privilegeId, customerId, privilegeType(typeDesc).name(),
            total, used, LocalDate.parse(validFrom), LocalDate.parse(validTo)));
    }

    @Given("優惠 {string} 有以下使用紀錄:")
    public void usageRecords(String privilegeId, DataTable table) {
        PrivilegeEntity entity = privilegeRepository.findById(privilegeId)
            .orElseThrow(() -> new IllegalStateException("優惠不存在：" + privilegeId));
        var records = new ArrayList<PrivilegeUsageRecordEntity>();
        for (Map<String, String> row : table.asMaps()) {
            records.add(new PrivilegeUsageRecordEntity(
                LocalDate.parse(row.get("使用日期")),
                new BigDecimal(row.get("節省金額")),
                row.get("目標帳號")));
        }
        entity.replaceUsageRecords(records);
        privilegeRepository.save(entity);
    }

    private static PrivilegeType privilegeType(String description) {
        for (PrivilegeType t : PrivilegeType.values()) {
            if (t.description().equals(description)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知的優惠類型：" + description);
    }
}
