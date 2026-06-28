package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.account.AccountStatus;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(AccountId accountId, AccountStatus status) {
        super("帳戶 [%s] 狀態為 [%s]，無法查詢".formatted(accountId.value(), status));
    }
}
