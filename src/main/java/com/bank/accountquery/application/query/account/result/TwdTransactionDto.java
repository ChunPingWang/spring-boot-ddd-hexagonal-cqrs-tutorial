package com.bank.accountquery.application.query.account.result;

import com.bank.accountquery.domain.model.account.Transaction;

public record TwdTransactionDto(
    String transactionId,
    String transactionDate,
    String transactionType,
    String amount,
    String description,
    String channel
) {
    public static TwdTransactionDto from(Transaction t) {
        return new TwdTransactionDto(
            t.getTransactionId().value(),
            t.transactionDate().toString(),
            t.getType().name(),
            t.getAmount().amount().toPlainString(),
            t.getDescription(),
            t.getChannel().name()
        );
    }
}
