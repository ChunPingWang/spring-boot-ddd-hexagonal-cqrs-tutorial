package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.account.AccountId;

public class AccountCurrencyMismatchException extends RuntimeException {
    public AccountCurrencyMismatchException(AccountId accountId) {
        super("帳戶 [%s] 幣別與帳戶型態不符".formatted(accountId.value()));
    }
}
