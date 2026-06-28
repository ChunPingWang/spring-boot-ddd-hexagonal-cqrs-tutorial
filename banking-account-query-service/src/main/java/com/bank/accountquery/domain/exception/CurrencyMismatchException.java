package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.shared.Currency;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(Currency expected, Currency actual) {
        super("幣別不一致：[%s] 與 [%s] 無法運算".formatted(expected, actual));
    }
}
