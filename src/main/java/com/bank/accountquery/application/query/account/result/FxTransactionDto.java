package com.bank.accountquery.application.query.account.result;

import com.bank.accountquery.domain.model.account.Transaction;

/**
 * 外幣交易紀錄 Read Model（多呈現原幣/匯率/台幣等值）。
 */
public record FxTransactionDto(
    String transactionId,
    String transactionDate,
    String transactionType,
    String currencyCode,
    String fxAmount,
    String twdEquivalent,
    String exchangeRate,
    String description
) {
    public static FxTransactionDto from(Transaction t) {
        var twdEq = t.getTwdEquivalent();
        var rate = t.exchangeRate();
        return new FxTransactionDto(
            t.getTransactionId().value(),
            t.transactionDate().toString(),
            t.getType().name(),
            t.getAmount().currency().name(),
            t.getAmount().amount().toPlainString(),
            twdEq == null ? null : twdEq.amount().toPlainString(),
            rate == null ? null : rate.toPlainString(),
            t.getDescription()
        );
    }
}
