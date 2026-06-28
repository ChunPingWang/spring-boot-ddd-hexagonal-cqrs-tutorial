package com.bank.accountquery.infrastructure.adapter.out.eventstore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 事件庫的一筆事件（append-only）。以 union 欄位承載兩種事件類型，
 * 由 event_type 區分。正式系統通常改用不透明的 JSON/Avro payload + schema registry。
 */
@Entity
@Table(name = "privilege_event")
public class PrivilegeEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "occurred_on", nullable = false)
    private LocalDateTime occurredOn;

    @Column(name = "subject_id")
    private String subjectId;

    @Column(name = "priv_type")
    private String privType;

    @Column(name = "total_quota")
    private Integer totalQuota;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "saved_amount")
    private BigDecimal savedAmount;

    @Column(name = "used_date")
    private LocalDate usedDate;

    @Column(name = "target_account_no")
    private String targetAccountNo;

    @Column(name = "remaining_quota")
    private Integer remainingQuota;

    protected PrivilegeEventEntity() {
    }

    public PrivilegeEventEntity(String aggregateId, int sequenceNo, String eventType, LocalDateTime occurredOn) {
        this.aggregateId = aggregateId;
        this.sequenceNo = sequenceNo;
        this.eventType = eventType;
        this.occurredOn = occurredOn;
    }

    public String getAggregateId() { return aggregateId; }
    public int getSequenceNo()     { return sequenceNo; }
    public String getEventType()   { return eventType; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
    public String getSubjectId()   { return subjectId; }
    public String getPrivType()    { return privType; }
    public Integer getTotalQuota() { return totalQuota; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo()  { return validTo; }
    public BigDecimal getSavedAmount() { return savedAmount; }
    public LocalDate getUsedDate() { return usedDate; }
    public String getTargetAccountNo() { return targetAccountNo; }
    public Integer getRemainingQuota() { return remainingQuota; }

    public void setSubjectId(String v)   { this.subjectId = v; }
    public void setPrivType(String v)    { this.privType = v; }
    public void setTotalQuota(Integer v) { this.totalQuota = v; }
    public void setValidFrom(LocalDate v) { this.validFrom = v; }
    public void setValidTo(LocalDate v)  { this.validTo = v; }
    public void setSavedAmount(BigDecimal v) { this.savedAmount = v; }
    public void setUsedDate(LocalDate v) { this.usedDate = v; }
    public void setTargetAccountNo(String v) { this.targetAccountNo = v; }
    public void setRemainingQuota(Integer v) { this.remainingQuota = v; }
}
