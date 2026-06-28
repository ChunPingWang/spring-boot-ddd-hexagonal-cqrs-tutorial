package com.bank.accountquery.domain.exception;

public class UnsupportedCurrencyException extends RuntimeException {
    public UnsupportedCurrencyException(String code) {
        super("不支援的幣別：[%s]".formatted(code));
    }
}
