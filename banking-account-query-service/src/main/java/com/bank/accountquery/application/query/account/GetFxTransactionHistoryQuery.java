package com.bank.accountquery.application.query.account;

import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;

public record GetFxTransactionHistoryQuery(
    CustomerId customerId,
    AccountId accountId,
    Currency currency,
    DateRange dateRange,
    int page,
    int size
) {}
