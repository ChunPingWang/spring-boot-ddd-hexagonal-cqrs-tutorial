package com.bank.accountquery.domain.model.account;

import com.bank.accountquery.domain.exception.InvalidAccountIdFormatException;
import java.util.Objects;

/**
 * AccountId — 封裝帳號格式驗證（14 位數字）。
 */
public record AccountId(String value) {

    public AccountId {
        Objects.requireNonNull(value);
        if (!value.matches("\\d{14}")) {
            throw new InvalidAccountIdFormatException("帳號格式不正確，需為 14 位數字");
        }
    }

    public static AccountId of(String value) {
        return new AccountId(value);
    }
}
