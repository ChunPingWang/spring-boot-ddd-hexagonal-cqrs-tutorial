package com.bank.accountquery.application.query.privilege.result;

import com.bank.accountquery.domain.model.privilege.TransferPrivilege;

/**
 * 轉帳優惠 Read Model — 讀取 Aggregate 計算後的值（isValid、remainingQuota）。
 */
public record TransferPrivilegeDto(
    String privilegeId,
    String privilegeType,
    String description,
    int totalQuota,
    int usedQuota,
    int remainingQuota,
    String validFrom,
    String validTo,
    boolean isValid,
    boolean expired
) {
    public static TransferPrivilegeDto from(TransferPrivilege p) {
        return new TransferPrivilegeDto(
            p.getPrivilegeId().value(),
            p.getType().name(),
            p.getType().description(),
            p.getTotalQuota(),
            p.getUsedQuota(),
            p.getRemainingQuota(),          // Domain Method
            p.getValidPeriod().startDate().toString(),
            p.getValidPeriod().endDate().toString(),
            p.isValid(),                    // Domain Method
            p.isExpired()                   // Domain Method
        );
    }
}
