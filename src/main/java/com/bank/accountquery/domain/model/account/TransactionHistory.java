package com.bank.accountquery.domain.model.account;

import com.bank.accountquery.domain.model.shared.DateRange;
import java.util.List;
import java.util.Objects;

/**
 * TransactionHistory — Value Object（查詢結果封裝，不可變）。
 */
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
