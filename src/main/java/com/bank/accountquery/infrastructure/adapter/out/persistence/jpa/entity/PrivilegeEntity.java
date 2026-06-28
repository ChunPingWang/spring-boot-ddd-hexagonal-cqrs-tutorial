package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity — 對應 transfer_privilege 資料表，並以 @OneToMany（cascade + orphanRemoval）
 * 持有 usage records，使「以 Aggregate Root 為單位儲存」得以一次寫入整個邊界（見 ADR-001）。
 */
@Entity
@Table(name = "transfer_privilege")
public class PrivilegeEntity {

    @Id
    @Column(name = "privilege_id")
    private String privilegeId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "total_quota", nullable = false)
    private int totalQuota;

    @Column(name = "used_quota", nullable = false)
    private int usedQuota;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @OneToMany(mappedBy = "privilege", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PrivilegeUsageRecordEntity> usageRecords = new ArrayList<>();

    protected PrivilegeEntity() {
    }

    public PrivilegeEntity(String privilegeId, String ownerId, String type,
                           int totalQuota, int usedQuota, LocalDate validFrom, LocalDate validTo) {
        this.privilegeId = privilegeId;
        this.ownerId = ownerId;
        this.type = type;
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public void setUsedQuota(int usedQuota) {
        this.usedQuota = usedQuota;
    }

    public void replaceUsageRecords(List<PrivilegeUsageRecordEntity> records) {
        this.usageRecords.clear();               // orphanRemoval 會刪除舊紀錄
        for (PrivilegeUsageRecordEntity r : records) {
            r.setPrivilege(this);
            this.usageRecords.add(r);
        }
    }

    public String getPrivilegeId() { return privilegeId; }
    public String getOwnerId()     { return ownerId; }
    public String getType()        { return type; }
    public int getTotalQuota()     { return totalQuota; }
    public int getUsedQuota()      { return usedQuota; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo()  { return validTo; }
    public List<PrivilegeUsageRecordEntity> getUsageRecords() { return usageRecords; }
}
