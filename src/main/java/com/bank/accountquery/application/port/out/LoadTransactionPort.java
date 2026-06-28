package com.bank.accountquery.application.port.out;

import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.shared.DateRange;
import java.util.List;

/**
 * Output Port — 取得原始交易資料（已於 DB 層初步過濾），
 * 由 Account Aggregate 執行最終過濾與業務驗證（見 ADR-002 規則 1）。
 */
public interface LoadTransactionPort {
    List<Transaction> findByAccountId(AccountId accountId, DateRange dateRange);
}
