package com.bank.accountquery.bdd;

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
import com.bank.accountquery.infrastructure.adapter.out.persistence.inmemory.InMemoryBankingDataStore;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Given 步驟：在資料源中佈置每個情境所需的帳戶、交易、優惠與認證身分。
 */
public class DataSetupSteps {

    private final InMemoryBankingDataStore store;
    private final ScenarioContext context;

    public DataSetupSteps(InMemoryBankingDataStore store, ScenarioContext context) {
        this.store = store;
        this.context = context;
    }

    @Given("客戶 {string} 已完成身份認證")
    public void customerAuthenticated(String customerId) {
        context.customerId = customerId;
    }

    @Given("客戶 {string} 有一個啟用的台幣帳戶 {string}")
    public void activeTwdAccount(String customerId, String accountId) {
        store.addAccount(new Account(new AccountId(accountId), CustomerId.of(customerId),
            AccountType.TWD, Currency.TWD, AccountStatus.ACTIVE));
    }

    @Given("客戶 {string} 有一個凍結的台幣帳戶 {string}")
    public void frozenTwdAccount(String customerId, String accountId) {
        store.addAccount(new Account(new AccountId(accountId), CustomerId.of(customerId),
            AccountType.TWD, Currency.TWD, AccountStatus.FROZEN));
    }

    @Given("客戶 {string} 有一個啟用的美元帳戶 {string}")
    public void activeUsdAccount(String customerId, String accountId) {
        store.addAccount(new Account(new AccountId(accountId), CustomerId.of(customerId),
            AccountType.FX, Currency.USD, AccountStatus.ACTIVE));
    }

    @Given("帳戶 {string} 有以下交易:")
    public void twdTransactions(String accountId, DataTable table) {
        var list = new ArrayList<Transaction>();
        int i = 0;
        for (Map<String, String> row : table.asMaps()) {
            list.add(new Transaction(
                TransactionId.of(accountId + "-T" + (i++)),
                "存入".equals(row.get("類型")) ? TransactionType.CREDIT : TransactionType.DEBIT,
                Money.twd(new BigDecimal(row.get("金額"))),
                null,
                LocalDate.parse(row.get("日期")).atStartOfDay(),
                row.get("說明"),
                TransactionChannel.SYSTEM));
        }
        store.putTransactions(new AccountId(accountId), list);
    }

    @Given("帳戶 {string} 有以下美元交易:")
    public void fxTransactions(String accountId, DataTable table) {
        var list = new ArrayList<Transaction>();
        int i = 0;
        for (Map<String, String> row : table.asMaps()) {
            var fx = new BigDecimal(row.get("原幣金額"));
            var rate = new BigDecimal(row.get("匯率"));
            var twd = Money.twd(fx.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            list.add(new Transaction(
                TransactionId.of(accountId + "-F" + (i++)),
                "存入".equals(row.get("類型")) ? TransactionType.CREDIT : TransactionType.DEBIT,
                Money.of(fx, Currency.USD),
                twd,
                LocalDate.parse(row.get("日期")).atStartOfDay(),
                row.get("說明"),
                TransactionChannel.ONLINE));
        }
        store.putTransactions(new AccountId(accountId), list);
    }

    @Given("客戶 {string} 有一項優惠 {string} 類型 {string} 總次數 {int} 已用 {int} 有效期 {string} 至 {string}")
    public void privilege(String customerId, String privilegeId, String typeDesc,
                          int total, int used, String validFrom, String validTo) {
        store.addPrivilege(new TransferPrivilege(
            PrivilegeId.of(privilegeId),
            CustomerId.of(customerId),
            privilegeType(typeDesc),
            total, used,
            new DateRange(LocalDate.parse(validFrom), LocalDate.parse(validTo)),
            List.of()));
    }

    @Given("優惠 {string} 有以下使用紀錄:")
    public void usageRecords(String privilegeId, DataTable table) {
        var records = new ArrayList<PrivilegeUsageRecord>();
        for (Map<String, String> row : table.asMaps()) {
            records.add(new PrivilegeUsageRecord(
                LocalDate.parse(row.get("使用日期")),
                Money.twd(new BigDecimal(row.get("節省金額"))),
                row.get("目標帳號")));
        }
        store.replaceUsageRecords(PrivilegeId.of(privilegeId), records);
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
