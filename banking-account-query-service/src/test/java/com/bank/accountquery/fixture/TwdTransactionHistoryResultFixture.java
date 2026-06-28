package com.bank.accountquery.fixture;

import com.bank.accountquery.application.query.account.result.TwdTransactionDto;
import com.bank.accountquery.application.query.account.result.TwdTransactionHistoryResult;
import com.bank.accountquery.application.query.common.PageInfo;
import java.util.List;

public final class TwdTransactionHistoryResultFixture {

    private TwdTransactionHistoryResultFixture() {}

    public static TwdTransactionHistoryResult sample() {
        var dtos = List.of(
            new TwdTransactionDto("T001", "2025-01-05T09:00", "CREDIT", "50000.00", "薪資轉帳", "SYSTEM"),
            new TwdTransactionDto("T002", "2025-01-10T14:30", "DEBIT", "10000.00", "ATM 提款", "ATM"),
            new TwdTransactionDto("T003", "2025-01-20T00:00", "CREDIT", "3000.00", "利息入帳", "SYSTEM")
        );
        return new TwdTransactionHistoryResult("00123456789012", dtos, PageInfo.of(0, 20, 3));
    }
}
