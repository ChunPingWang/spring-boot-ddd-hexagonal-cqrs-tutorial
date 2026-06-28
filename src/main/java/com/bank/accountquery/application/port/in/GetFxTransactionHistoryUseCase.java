package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.query.account.GetFxTransactionHistoryQuery;
import com.bank.accountquery.application.query.account.result.FxTransactionHistoryResult;

public interface GetFxTransactionHistoryUseCase {
    FxTransactionHistoryResult execute(GetFxTransactionHistoryQuery query);
}
