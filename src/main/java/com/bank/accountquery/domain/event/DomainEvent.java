package com.bank.accountquery.domain.event;

import java.time.LocalDateTime;

/**
 * Domain Event — 表示「領域中已經發生的事實」（過去式命名）。
 * 由 Aggregate 在狀態改變時記錄，再由 Application Layer 透過 Output Port 發布。
 */
public interface DomainEvent {
    LocalDateTime occurredOn();
}
