package com.bank.accountquery.application.query.account.result;

import com.bank.accountquery.application.query.common.PageInfo;
import com.bank.accountquery.application.query.common.Pagination;
import com.bank.accountquery.domain.model.account.TransactionHistory;
import com.bank.accountquery.domain.model.shared.Currency;
import java.util.List;

/**
 * 外幣交易紀錄 Read Model。
 */
public record FxTransactionHistoryResult(
    String accountId,
    String currencyCode,
    List<FxTransactionDto> transactions,
    PageInfo pageInfo
) {
    public static FxTransactionHistoryResult from(TransactionHistory history,
                                                  Currency currency,
                                                  int page, int size) {
        var dtos = history.transactions().stream()
            .map(FxTransactionDto::from)
            .toList();
        return new FxTransactionHistoryResult(
            history.accountId().value(),
            currency.name(),
            Pagination.paginate(dtos, page, size),
            PageInfo.of(page, size, dtos.size())
        );
    }
}
