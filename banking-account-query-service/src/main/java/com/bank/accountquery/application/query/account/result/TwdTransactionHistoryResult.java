package com.bank.accountquery.application.query.account.result;

import com.bank.accountquery.application.query.common.PageInfo;
import com.bank.accountquery.application.query.common.Pagination;
import com.bank.accountquery.domain.model.account.TransactionHistory;
import java.util.List;

/**
 * 台幣交易紀錄 Read Model。
 */
public record TwdTransactionHistoryResult(
    String accountId,
    List<TwdTransactionDto> transactions,
    PageInfo pageInfo
) {
    public static TwdTransactionHistoryResult from(TransactionHistory history,
                                                   int page, int size) {
        var dtos = history.transactions().stream()
            .map(TwdTransactionDto::from)
            .toList();
        return new TwdTransactionHistoryResult(
            history.accountId().value(),
            Pagination.paginate(dtos, page, size),
            PageInfo.of(page, size, dtos.size())
        );
    }
}
