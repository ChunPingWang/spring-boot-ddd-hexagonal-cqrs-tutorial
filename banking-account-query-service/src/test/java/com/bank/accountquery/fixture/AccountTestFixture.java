package com.bank.accountquery.fixture;

import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.account.AccountStatus;
import com.bank.accountquery.domain.model.account.AccountType;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;

public final class AccountTestFixture {

    public static final CustomerId DEFAULT_OWNER = CustomerId.of("C001");
    public static final AccountId DEFAULT_TWD_ID = new AccountId("00123456789012");
    public static final AccountId DEFAULT_FX_ID = new AccountId("00123456789013");

    private AccountTestFixture() {}

    public static Account activeTwdAccount() {
        return activeTwdAccount(DEFAULT_OWNER);
    }

    public static Account activeTwdAccount(CustomerId owner) {
        return new Account(DEFAULT_TWD_ID, owner, AccountType.TWD, Currency.TWD, AccountStatus.ACTIVE);
    }

    public static Account frozenTwdAccount(CustomerId owner) {
        return new Account(DEFAULT_TWD_ID, owner, AccountType.TWD, Currency.TWD, AccountStatus.FROZEN);
    }

    public static Account activeFxAccount(CustomerId owner) {
        return new Account(DEFAULT_FX_ID, owner, AccountType.FX, Currency.USD, AccountStatus.ACTIVE);
    }
}
