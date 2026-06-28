package com.bank.accountquery.application.port.out;

import com.bank.accountquery.domain.event.DomainEvent;
import java.util.List;

/**
 * Output Port — 事件溯源的事件庫（append-only）。
 * 以 (aggregateId, 版本) 做樂觀並行控制：版本即該聚合已存在的事件數。
 */
public interface EventStorePort {

    /** 載入某聚合的完整事件流（依序）。 */
    List<DomainEvent> load(String aggregateId);

    /** 目前版本（已存在的事件數）。 */
    int currentVersion(String aggregateId);

    /**
     * 附加新事件。expectedVersion 必須等於目前版本，否則拋出並行衝突。
     */
    void append(String aggregateId, int expectedVersion, List<DomainEvent> newEvents);
}
