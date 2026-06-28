package com.bank.accountquery.application.command.privilege.result;

import com.bank.accountquery.domain.event.DomainEvent;
import com.bank.accountquery.domain.event.TransferPrivilegeGrantedEvent;
import com.bank.accountquery.domain.event.TransferPrivilegeUsedEvent;

/**
 * 事件流的可讀檢視（讓人看見「append-only 的事實序列」）。
 */
public record PrivilegeEventView(
    int sequenceNo,
    String eventType,
    String occurredOn,
    String detail
) {
    public static PrivilegeEventView from(int sequenceNo, DomainEvent event) {
        return switch (event) {
            case TransferPrivilegeGrantedEvent e -> new PrivilegeEventView(
                sequenceNo, "TransferPrivilegeGranted", e.occurredOn().toString(),
                "核發 %s 次、有效期 %s ~ %s".formatted(e.totalQuota(),
                    e.validPeriod().startDate(), e.validPeriod().endDate()));
            case TransferPrivilegeUsedEvent e -> new PrivilegeEventView(
                sequenceNo, "TransferPrivilegeUsed", e.occurredOn().toString(),
                "節省 %s、轉帳至 %s、剩餘 %d".formatted(
                    e.savedAmount().amount().toPlainString(), e.targetAccountNo(), e.remainingQuota()));
            default -> new PrivilegeEventView(sequenceNo, event.getClass().getSimpleName(),
                event.occurredOn().toString(), "");
        };
    }
}
