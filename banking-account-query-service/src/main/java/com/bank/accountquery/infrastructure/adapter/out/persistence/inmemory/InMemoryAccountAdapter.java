package com.bank.accountquery.infrastructure.adapter.out.persistence.inmemory;

import com.bank.accountquery.application.port.out.LoadAccountPort;
import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Driven Adapter — 實作 LoadAccountPort（記憶體版）。
 */
@Repository
public class InMemoryAccountAdapter implements LoadAccountPort {

    private final InMemoryBankingDataStore store;

    public InMemoryAccountAdapter(InMemoryBankingDataStore store) {
        this.store = store;
    }

    @Override
    public Optional<Account> findByAccountId(AccountId accountId) {
        return Optional.ofNullable(store.accounts().get(accountId));
    }

    @Override
    public List<Account> findAllByCustomerId(CustomerId customerId) {
        return store.accounts().values().stream()
            .filter(a -> a.isOwnedBy(customerId))
            .toList();
    }
}
