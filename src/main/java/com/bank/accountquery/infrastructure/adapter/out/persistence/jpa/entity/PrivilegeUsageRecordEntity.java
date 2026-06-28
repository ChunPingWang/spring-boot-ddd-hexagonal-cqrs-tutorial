package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "privilege_usage_record")
public class PrivilegeUsageRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "privilege_id", nullable = false)
    private PrivilegeEntity privilege;

    @Column(name = "used_date", nullable = false)
    private LocalDate usedDate;

    @Column(name = "saved_amount", nullable = false)
    private BigDecimal savedAmount;

    @Column(name = "target_account_no", nullable = false)
    private String targetAccountNo;

    protected PrivilegeUsageRecordEntity() {
    }

    public PrivilegeUsageRecordEntity(LocalDate usedDate, BigDecimal savedAmount, String targetAccountNo) {
        this.usedDate = usedDate;
        this.savedAmount = savedAmount;
        this.targetAccountNo = targetAccountNo;
    }

    void setPrivilege(PrivilegeEntity privilege) {
        this.privilege = privilege;
    }

    public LocalDate getUsedDate()       { return usedDate; }
    public BigDecimal getSavedAmount()   { return savedAmount; }
    public String getTargetAccountNo()   { return targetAccountNo; }
}
