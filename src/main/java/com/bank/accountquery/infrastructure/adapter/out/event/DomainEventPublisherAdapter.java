package com.bank.accountquery.infrastructure.adapter.out.event;

import com.bank.accountquery.application.port.out.DomainEventPublisher;
import com.bank.accountquery.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Driven Adapter — 實作 DomainEventPublisher。
 * 一方面記稽核日誌，一方面把領域事件轉發到 Spring 的事件系統，
 * 讓任意 {@code @EventListener} 解耦地消費（pub/sub）。
 * 正式版可改為發到 Kafka / Outbox；Application Layer 無需改動。
 */
@Component
public class DomainEventPublisherAdapter implements DomainEventPublisher {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final ApplicationEventPublisher springPublisher;

    public DomainEventPublisherAdapter(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        AUDIT.info("領域事件發布：{}", event);
        springPublisher.publishEvent(event);
    }
}
