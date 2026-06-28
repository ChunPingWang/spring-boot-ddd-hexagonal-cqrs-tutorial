package com.bank.accountquery.infrastructure.adapter.out.event;

import com.bank.accountquery.application.port.out.DomainEventPublisher;
import com.bank.accountquery.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Driven Adapter — 實作 DomainEventPublisher（以稽核日誌方式發布）。
 * 正式版可替換為 Spring ApplicationEventPublisher、Kafka、Outbox 等，Handler 無需改動。
 */
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    @Override
    public void publish(DomainEvent event) {
        AUDIT.info("領域事件發布：{}", event);
    }
}
