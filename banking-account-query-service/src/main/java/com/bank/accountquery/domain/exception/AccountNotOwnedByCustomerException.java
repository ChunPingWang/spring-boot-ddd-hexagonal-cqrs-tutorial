package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.CustomerId;

public class AccountNotOwnedByCustomerException extends RuntimeException {
    public AccountNotOwnedByCustomerException(AccountId accountId, CustomerId customerId) {
        super("帳戶 [%s] 不屬於客戶 [%s]".formatted(accountId.value(), customerId.value()));
    }
}
