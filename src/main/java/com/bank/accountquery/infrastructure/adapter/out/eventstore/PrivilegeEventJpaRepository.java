package com.bank.accountquery.infrastructure.adapter.out.eventstore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeEventJpaRepository extends JpaRepository<PrivilegeEventEntity, Long> {
    List<PrivilegeEventEntity> findByAggregateIdOrderBySequenceNoAsc(String aggregateId);
    int countByAggregateId(String aggregateId);
}
