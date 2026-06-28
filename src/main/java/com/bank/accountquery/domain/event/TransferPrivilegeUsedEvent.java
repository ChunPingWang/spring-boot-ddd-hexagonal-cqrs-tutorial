package com.bank.accountquery.domain.event;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.Money;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 「轉帳優惠已被使用一次」領域事件。攜帶重播狀態所需的完整資訊（含目標帳號）。
 */
public record TransferPrivilegeUsedEvent(
    PrivilegeId privilegeId,
    CustomerId customerId,
    Money savedAmount,
    String targetAccountNo,
    LocalDate usedDate,
    int remainingQuota,
    LocalDateTime occurredOn
) implements DomainEvent {

    public TransferPrivilegeUsedEvent(PrivilegeId privilegeId, CustomerId customerId, Money savedAmount,
                                      String targetAccountNo, LocalDate usedDate, int remainingQuota) {
        this(privilegeId, customerId, savedAmount, targetAccountNo, usedDate, remainingQuota, LocalDateTime.now());
    }
}
