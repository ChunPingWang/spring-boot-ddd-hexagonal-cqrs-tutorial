package com.bank.accountquery.application.command.privilege.result;

import com.bank.accountquery.domain.model.privilege.TransferPrivilege;

/**
 * 由事件流重播後得到的優惠狀態（讀取-by-replay 的結果）。
 */
public record EventSourcedPrivilegeState(
    String privilegeId,
    String ownerId,
    String type,
    int totalQuota,
    int usedQuota,
    int remainingQuota,
    String validFrom,
    String validTo,
    boolean valid
) {
    public static EventSourcedPrivilegeState from(TransferPrivilege p) {
        return new EventSourcedPrivilegeState(
            p.getPrivilegeId().value(),
            p.getOwnerId().value(),
            p.getType().name(),
            p.getTotalQuota(),
            p.getUsedQuota(),
            p.getRemainingQuota(),
            p.getValidPeriod().startDate().toString(),
            p.getValidPeriod().endDate().toString(),
            p.isValid());
    }
}
