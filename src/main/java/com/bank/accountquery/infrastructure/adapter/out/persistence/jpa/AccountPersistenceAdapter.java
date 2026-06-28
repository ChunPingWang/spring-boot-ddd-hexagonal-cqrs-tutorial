package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa;

import com.bank.accountquery.application.port.out.LoadAccountPort;
import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.AccountJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Driven Adapter — 以 JPA/PostgreSQL 實作 LoadAccountPort。
 */
@Repository
public class AccountPersistenceAdapter implements LoadAccountPort {

    private final AccountJpaRepository repository;

    public AccountPersistenceAdapter(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findByAccountId(AccountId accountId) {
        return repository.findById(accountId.value()).map(PersistenceMappers::toDomain);
    }

    @Override
    public List<Account> findAllByCustomerId(CustomerId customerId) {
        return repository.findByOwnerId(customerId.value()).stream()
            .map(PersistenceMappers::toDomain)
            .toList();
    }
}
