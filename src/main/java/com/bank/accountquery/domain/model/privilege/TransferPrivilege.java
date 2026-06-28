package com.bank.accountquery.domain.model.privilege;

import com.bank.accountquery.domain.event.DomainEvent;
import com.bank.accountquery.domain.event.TransferPrivilegeUsedEvent;
import com.bank.accountquery.domain.exception.PrivilegeExpiredException;
import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.PrivilegeQuotaExhaustedException;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TransferPrivilege — Aggregate Root。
 * 讀取側：計算有效性、剩餘次數、過濾使用紀錄。
 * 寫入側：use() 在守護不變式（所有權、有效期、剩餘額度）後改變狀態並記錄領域事件。
 */
public class TransferPrivilege {

    private final PrivilegeId privilegeId;
    private final CustomerId ownerId;
    private final PrivilegeType type;
    private final int totalQuota;
    private int usedQuota;                                   // 寫入側會改變
    private final DateRange validPeriod;
    private final List<PrivilegeUsageRecord> usageRecords;  // 寫入側會新增
    private final List<DomainEvent> domainEvents = new ArrayList<>();

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
        this.usageRecords = new ArrayList<>(usageRecords);
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

    // ── 業務規則 3：使用紀錄過濾（讀取側）──────────────────────────
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

    // ── 寫入側行為：使用一次優惠 ────────────────────────────────────
    // 一致性邊界：所有不變式都在此守護，外界無法繞過 Aggregate Root 直接改狀態。
    public void use(CustomerId requesterId, Money savedAmount, String targetAccountNo) {
        verifyOwnership(requesterId);
        if (!isWithinValidPeriod()) {
            throw new PrivilegeExpiredException(this.privilegeId);
        }
        if (!hasRemainingQuota()) {
            throw new PrivilegeQuotaExhaustedException(this.privilegeId);
        }
        this.usedQuota += 1;
        var record = new PrivilegeUsageRecord(LocalDate.now(), savedAmount, targetAccountNo);
        this.usageRecords.add(record);
        this.domainEvents.add(new TransferPrivilegeUsedEvent(
            this.privilegeId, this.ownerId, savedAmount, record.usedDate(), getRemainingQuota()));
    }

    /** 取出並清空尚未發布的領域事件（由 Application Layer 於儲存後發布）。 */
    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public PrivilegeId getPrivilegeId() { return privilegeId; }
    public CustomerId getOwnerId()      { return ownerId; }
    public int getTotalQuota()          { return totalQuota; }
    public int getUsedQuota()           { return usedQuota; }
    public DateRange getValidPeriod()   { return validPeriod; }
    public PrivilegeType getType()      { return type; }

    /** 供持久化映射使用（以 Aggregate Root 為單位儲存整個邊界內資料）。 */
    public List<PrivilegeUsageRecord> getUsageRecords() {
        return List.copyOf(usageRecords);
    }
}
