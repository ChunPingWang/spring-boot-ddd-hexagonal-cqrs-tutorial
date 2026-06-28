package com.bank.accountquery.application.command.privilege.result;

import com.bank.accountquery.domain.model.privilege.TransferPrivilege;

/**
 * 使用優惠後的結果 Read Model（回報使用後狀態）。
 */
public record UseTransferPrivilegeResult(
    String privilegeId,
    int usedQuota,
    int remainingQuota
) {
    public static UseTransferPrivilegeResult from(TransferPrivilege p) {
        return new UseTransferPrivilegeResult(
            p.getPrivilegeId().value(),
            p.getUsedQuota(),
            p.getRemainingQuota());
    }
}
