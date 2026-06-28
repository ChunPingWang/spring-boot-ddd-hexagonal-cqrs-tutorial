package com.bank.accountquery.fixture;

import com.bank.accountquery.application.query.account.GetFxTransactionHistoryQuery;
import com.bank.accountquery.application.query.account.GetTwdTransactionHistoryQuery;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import java.time.LocalDate;

public final class QueryFixture {

    private QueryFixture() {}

    public static GetTwdTransactionHistoryQuery twdQuery(String customerId, String accountId) {
        return new GetTwdTransactionHistoryQuery(
            CustomerId.of(customerId),
            new AccountId(accountId),
            new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
            0, 20
        );
    }

    public static GetFxTransactionHistoryQuery fxQuery(String customerId, String accountId, Currency currency) {
        return new GetFxTransactionHistoryQuery(
            CustomerId.of(customerId),
            new AccountId(accountId),
            currency,
            new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
            0, 20
        );
    }
}
