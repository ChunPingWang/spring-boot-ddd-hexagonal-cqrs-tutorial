package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository;

import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.TransactionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {
    // 由 DB 層先以日期區間過濾（見 ADR-002 方案 A），最終業務過濾仍由 Account Aggregate 執行。
    List<TransactionEntity> findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAsc(
        String accountId, LocalDateTime start, LocalDateTime end);
}
