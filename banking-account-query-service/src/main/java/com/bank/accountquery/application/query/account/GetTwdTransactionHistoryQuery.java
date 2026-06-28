package com.bank.accountquery.application.query.account;

import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;

public record GetTwdTransactionHistoryQuery(
    CustomerId customerId,
    AccountId accountId,
    DateRange dateRange,
    int page,
    int size
) {}
