package com.bank.accountquery.domain.model.privilege;

import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * TransferPrivilege — Aggregate Root。
 * 優惠的有效性、剩餘次數等業務規則完全封裝於此。
 */
public class TransferPrivilege {

    private final PrivilegeId privilegeId;
    private final CustomerId ownerId;
    private final PrivilegeType type;
    private final int totalQuota;
    private final int usedQuota;
    private final DateRange validPeriod;
    private final List<PrivilegeUsageRecord> usageRecords;

    public TransferPrivilege(PrivilegeId privilegeId,
                             CustomerId ownerId,
                             PrivilegeType type,
                             int totalQuota,
                             int usedQuota,
                             DateRange validPeriod,
                             List<PrivilegeUsageRecord> usageRecords) {
        this.privilegeId = Objects.requireNonNull(privilegeId);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.type = Objects.requireNonNull(type);
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.validPeriod = Objects.requireNonNull(validPeriod);
        this.usageRecords = List.copyOf(usageRecords);
    }

    // ── 業務規則 1：優惠是否有效 ────────────────────────────────────
    public boolean isValid() {
        return isWithinValidPeriod() && hasRemainingQuota();
    }

    private boolean isWithinValidPeriod() {
        return validPeriod.contains(LocalDate.now());
    }

    private boolean hasRemainingQuota() {
        return getRemainingQuota() > 0;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(validPeriod.endDate());
    }

    // ── 業務規則 2：剩餘次數計算 ────────────────────────────────────
    public int getRemainingQuota() {
        return totalQuota - usedQuota;
    }

    // ── 業務規則 3：使用紀錄過濾 ────────────────────────────────────
    public PrivilegeUsageHistory filterUsageHistory(DateRange dateRange) {
        var filtered = usageRecords.stream()
            .filter(r -> dateRange.contains(r.usedDate()))
            .toList();
        return new PrivilegeUsageHistory(this.privilegeId, filtered, dateRange);
    }

    // ── 業務規則 4：所有權驗證 ──────────────────────────────────────
    public void verifyOwnership(CustomerId requesterId) {
        if (!this.ownerId.equals(requesterId)) {
            throw new PrivilegeNotOwnedByCustomerException(this.privilegeId, requesterId);
        }
    }

    public PrivilegeId getPrivilegeId() { return privilegeId; }
    public CustomerId getOwnerId()      { return ownerId; }
    public int getTotalQuota()          { return totalQuota; }
    public int getUsedQuota()           { return usedQuota; }
    public DateRange getValidPeriod()   { return validPeriod; }
    public PrivilegeType getType()      { return type; }
}
