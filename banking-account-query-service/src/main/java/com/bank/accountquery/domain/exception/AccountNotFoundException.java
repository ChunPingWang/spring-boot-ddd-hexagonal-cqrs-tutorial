package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.account.AccountId;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(AccountId accountId) {
        super("帳戶 [%s] 不存在".formatted(accountId.value()));
    }
}
