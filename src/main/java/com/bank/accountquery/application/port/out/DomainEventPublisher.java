package com.bank.accountquery.application.port.out;

import com.bank.accountquery.domain.event.DomainEvent;

/**
 * Output Port — 發布領域事件。由 Infrastructure 決定實際發布機制
 * （記憶體記錄、Spring 事件、訊息佇列…），Application Layer 不需知道。
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
