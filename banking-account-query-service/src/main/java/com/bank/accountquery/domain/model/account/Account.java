package com.bank.accountquery.domain.model.account;

import com.bank.accountquery.domain.exception.AccountCurrencyMismatchException;
import com.bank.accountquery.domain.exception.AccountNotActiveException;
import com.bank.accountquery.domain.exception.AccountNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.QueryRangeExceededException;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import java.util.List;
import java.util.Objects;

/**
 * Account — Aggregate Root。
 * 業務規則完全封裝於此，Application Layer 只協調，不判斷。
 */
public class Account {

    private final AccountId accountId;
    private final CustomerId ownerId;       // 帳戶持有人
    private final AccountType accountType;   // TWD / FX
    private final Currency currency;
    private final AccountStatus status;      // ACTIVE / FROZEN / CLOSED

    public Account(AccountId accountId,
                   CustomerId ownerId,
                   AccountType accountType,
                   Currency currency,
                   AccountStatus status) {
        this.accountId = Objects.requireNonNull(accountId);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.accountType = Objects.requireNonNull(accountType);
        this.currency = Objects.requireNonNull(currency);
        this.status = Objects.requireNonNull(status);
    }

    // ── 業務規則 1：所有權驗證 ──────────────────────────────────────
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

    // ── 業務規則 3：查詢區間限制（13 個月）與過濾 ───────────────────
    // 傳入的 transactions 由 Application Layer 透過 Output Port 取得後傳入，
    // Account 只負責執行業務規則，不知道資料從哪裡來。
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

    // ── Query Methods（給 Application Layer 讀取狀態用）──────────────
    public boolean isOwnedBy(CustomerId customerId) {
        return this.ownerId.equals(customerId);
    }

    public AccountId getAccountId()     { return accountId; }
    public CustomerId getOwnerId()      { return ownerId; }
    public AccountType getAccountType() { return accountType; }
    public Currency getCurrency()       { return currency; }
    public AccountStatus getStatus()    { return status; }
}
