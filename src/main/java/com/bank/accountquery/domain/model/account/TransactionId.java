package com.bank.accountquery.domain.model.account;

import java.util.Objects;

public record TransactionId(String value) {

    public TransactionId {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("TransactionId 不可為空");
        }
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }
}
