package com.bank.accountquery.domain.model.privilege;

import com.bank.accountquery.domain.event.DomainEvent;
import com.bank.accountquery.domain.event.TransferPrivilegeGrantedEvent;
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
 *
 * 此 Aggregate 同時支援兩種持久化風格（教學對照）：
 *  - 狀態儲存（state-stored）：用公開建構子載入目前狀態（JPA Adapter 走這條）。
 *  - 事件溯源（event-sourced）：以 grant()/use() 產生事件，rehydrate() 由事件重播重建狀態。
 *
 * 不論哪條路，業務不變式（所有權、有效期、剩餘額度）都只在 use() 內守護。
 */
public class TransferPrivilege {

    private PrivilegeId privilegeId;
    private CustomerId ownerId;
    private PrivilegeType type;
    private int totalQuota;
    private int usedQuota;
    private DateRange validPeriod;
    private final List<PrivilegeUsageRecord> usageRecords = new ArrayList<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** 狀態儲存載入用：直接帶入目前狀態。 */
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
        this.usageRecords.addAll(usageRecords);
    }

    private TransferPrivilege() {
        // 事件溯源重播用：先建立空殼，再由事件填入狀態。
    }

    // ── 事件溯源：核發（建立串流的第一個事件）──────────────────────
    public static TransferPrivilege grant(PrivilegeId privilegeId, CustomerId ownerId, PrivilegeType type,
                                          int totalQuota, DateRange validPeriod) {
        var privilege = new TransferPrivilege();
        privilege.recordAndApply(new TransferPrivilegeGrantedEvent(privilegeId, ownerId, type, totalQuota, validPeriod));
        return privilege;
    }

    // ── 事件溯源：以事件流重建狀態（不重新產生事件）────────────────
    public static TransferPrivilege rehydrate(List<DomainEvent> stream) {
        var privilege = new TransferPrivilege();
        for (DomainEvent event : stream) {
            privilege.apply(event);
        }
        return privilege;
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
    // 一致性邊界：先守護不變式，再「產生並套用」事件（state-stored 與 event-sourced 共用）。
    public void use(CustomerId requesterId, Money savedAmount, String targetAccountNo) {
        verifyOwnership(requesterId);
        if (!isWithinValidPeriod()) {
            throw new PrivilegeExpiredException(this.privilegeId);
        }
        if (!hasRemainingQuota()) {
            throw new PrivilegeQuotaExhaustedException(this.privilegeId);
        }
        recordAndApply(new TransferPrivilegeUsedEvent(
            this.privilegeId, requesterId, savedAmount, targetAccountNo,
            LocalDate.now(), getRemainingQuota() - 1));
    }

    // ── 事件套用（重建狀態的唯一途徑）──────────────────────────────
    private void apply(DomainEvent event) {
        switch (event) {
            case TransferPrivilegeGrantedEvent e -> {
                this.privilegeId = e.privilegeId();
                this.ownerId = e.ownerId();
                this.type = e.type();
                this.totalQuota = e.totalQuota();
                this.usedQuota = 0;
                this.validPeriod = e.validPeriod();
            }
            case TransferPrivilegeUsedEvent e -> {
                this.usedQuota += 1;
                this.usageRecords.add(new PrivilegeUsageRecord(e.usedDate(), e.savedAmount(), e.targetAccountNo()));
            }
            default -> throw new IllegalArgumentException("未知的領域事件：" + event.getClass().getName());
        }
    }

    private void recordAndApply(DomainEvent event) {
        apply(event);
        this.domainEvents.add(event);
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
