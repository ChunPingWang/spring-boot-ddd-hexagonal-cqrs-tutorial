package com.bank.accountquery.infrastructure.adapter.out.eventstore;

import com.bank.accountquery.application.port.out.EventStorePort;
import com.bank.accountquery.domain.event.DomainEvent;
import com.bank.accountquery.domain.event.TransferPrivilegeGrantedEvent;
import com.bank.accountquery.domain.event.TransferPrivilegeUsedEvent;
import com.bank.accountquery.domain.exception.ConcurrencyConflictException;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driven Adapter — 以關聯式資料表實作事件庫。
 * 以 (aggregate_id, sequence_no) 唯一鍵 + 版本比對達成樂觀並行控制。
 */
@Repository
public class EventStoreAdapter implements EventStorePort {

    private static final String GRANTED = "TransferPrivilegeGranted";
    private static final String USED = "TransferPrivilegeUsed";

    private final PrivilegeEventJpaRepository repository;

    public EventStoreAdapter(PrivilegeEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> load(String aggregateId) {
        return repository.findByAggregateIdOrderBySequenceNoAsc(aggregateId).stream()
            .map(EventStoreAdapter::toEvent)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int currentVersion(String aggregateId) {
        return repository.countByAggregateId(aggregateId);
    }

    @Override
    @Transactional
    public void append(String aggregateId, int expectedVersion, List<DomainEvent> newEvents) {
        int actual = repository.countByAggregateId(aggregateId);
        if (actual != expectedVersion) {
            throw new ConcurrencyConflictException(aggregateId, expectedVersion, actual);
        }
        var rows = new ArrayList<PrivilegeEventEntity>();
        int seq = expectedVersion;
        for (DomainEvent event : newEvents) {
            rows.add(toRow(aggregateId, ++seq, event));
        }
        repository.saveAll(rows);
    }

    private static PrivilegeEventEntity toRow(String aggregateId, int seq, DomainEvent event) {
        switch (event) {
            case TransferPrivilegeGrantedEvent e -> {
                var row = new PrivilegeEventEntity(aggregateId, seq, GRANTED, e.occurredOn());
                row.setSubjectId(e.ownerId().value());
                row.setPrivType(e.type().name());
                row.setTotalQuota(e.totalQuota());
                row.setValidFrom(e.validPeriod().startDate());
                row.setValidTo(e.validPeriod().endDate());
                return row;
            }
            case TransferPrivilegeUsedEvent e -> {
                var row = new PrivilegeEventEntity(aggregateId, seq, USED, e.occurredOn());
                row.setSubjectId(e.customerId().value());
                row.setSavedAmount(e.savedAmount().amount());
                row.setUsedDate(e.usedDate());
                row.setTargetAccountNo(e.targetAccountNo());
                row.setRemainingQuota(e.remainingQuota());
                return row;
            }
            default -> throw new IllegalArgumentException("無法序列化的事件：" + event.getClass().getName());
        }
    }

    private static DomainEvent toEvent(PrivilegeEventEntity row) {
        return switch (row.getEventType()) {
            case GRANTED -> new TransferPrivilegeGrantedEvent(
                PrivilegeId.of(row.getAggregateId()),
                CustomerId.of(row.getSubjectId()),
                PrivilegeType.valueOf(row.getPrivType()),
                row.getTotalQuota(),
                new DateRange(row.getValidFrom(), row.getValidTo()),
                row.getOccurredOn());
            case USED -> new TransferPrivilegeUsedEvent(
                PrivilegeId.of(row.getAggregateId()),
                CustomerId.of(row.getSubjectId()),
                Money.twd(row.getSavedAmount()),
                row.getTargetAccountNo(),
                row.getUsedDate(),
                row.getRemainingQuota(),
                row.getOccurredOn());
            default -> throw new IllegalArgumentException("未知的事件型別：" + row.getEventType());
        };
    }
}
