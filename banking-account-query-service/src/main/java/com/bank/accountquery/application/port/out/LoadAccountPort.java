package com.bank.accountquery.application.port.out;

import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import java.util.List;
import java.util.Optional;

/**
 * Output Port — 取得帳戶基本資料（不含交易明細，見 ADR-002 方案 A）。
 * 介面定義權在 Application Layer；由 Infrastructure 的 Driven Adapter 實作。
 */
public interface LoadAccountPort {
    Optional<Account> findByAccountId(AccountId accountId);
    List<Account> findAllByCustomerId(CustomerId customerId);
}
