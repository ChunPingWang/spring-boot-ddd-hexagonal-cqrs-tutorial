package com.bank.accountquery.domain.event;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import java.time.LocalDateTime;

/**
 * 「轉帳優惠已核發」領域事件 —— 事件溯源中一個優惠串流的第一個事件。
 */
public record TransferPrivilegeGrantedEvent(
    PrivilegeId privilegeId,
    CustomerId ownerId,
    PrivilegeType type,
    int totalQuota,
    DateRange validPeriod,
    LocalDateTime occurredOn
) implements DomainEvent {

    public TransferPrivilegeGrantedEvent(PrivilegeId privilegeId, CustomerId ownerId, PrivilegeType type,
                                         int totalQuota, DateRange validPeriod) {
        this(privilegeId, ownerId, type, totalQuota, validPeriod, LocalDateTime.now());
    }
}
