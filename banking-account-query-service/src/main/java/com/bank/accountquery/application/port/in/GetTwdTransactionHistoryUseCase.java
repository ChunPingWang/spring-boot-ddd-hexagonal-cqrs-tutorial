package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.query.account.GetTwdTransactionHistoryQuery;
import com.bank.accountquery.application.query.account.result.TwdTransactionHistoryResult;

public interface GetTwdTransactionHistoryUseCase {
    TwdTransactionHistoryResult execute(GetTwdTransactionHistoryQuery query);
}
