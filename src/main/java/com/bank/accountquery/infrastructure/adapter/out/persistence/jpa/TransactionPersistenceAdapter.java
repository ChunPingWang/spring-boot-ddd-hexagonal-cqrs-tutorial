package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa;

import com.bank.accountquery.application.port.out.LoadTransactionPort;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.TransactionJpaRepository;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Driven Adapter — 以 JPA/PostgreSQL 實作 LoadTransactionPort（DB 層先以日期區間過濾）。
 */
@Repository
public class TransactionPersistenceAdapter implements LoadTransactionPort {

    private final TransactionJpaRepository repository;

    public TransactionPersistenceAdapter(TransactionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Transaction> findByAccountId(AccountId accountId, DateRange dateRange) {
        var start = dateRange.startDate().atStartOfDay();
        var end = dateRange.endDate().atTime(LocalTime.MAX);
        return repository
            .findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAsc(accountId.value(), start, end)
            .stream()
            .map(PersistenceMappers::toDomain)
            .toList();
    }
}
