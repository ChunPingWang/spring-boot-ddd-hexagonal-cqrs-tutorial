package com.bank.accountquery.infrastructure.adapter.out.persistence.inmemory;

import com.bank.accountquery.application.port.out.LoadTransactionPort;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.shared.DateRange;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Driven Adapter — 實作 LoadTransactionPort（記憶體版）。
 * 模擬 DB 層的區間初步過濾；最終業務過濾交由 Account Aggregate（見 ADR-002 規則 1）。
 */
@Repository
public class InMemoryTransactionAdapter implements LoadTransactionPort {

    private final InMemoryBankingDataStore store;

    public InMemoryTransactionAdapter(InMemoryBankingDataStore store) {
        this.store = store;
    }

    @Override
    public List<Transaction> findByAccountId(AccountId accountId, DateRange dateRange) {
        return store.transactions().getOrDefault(accountId, List.of()).stream()
            .filter(t -> dateRange.contains(t.transactionDate().toLocalDate()))
            .toList();
    }
}
