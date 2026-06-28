package com.bank.accountquery.infrastructure.adapter.out.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "privilege_audit_log")
public class PrivilegeAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "privilege_id", nullable = false)
    private String privilegeId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "occurred_on", nullable = false)
    private LocalDateTime occurredOn;

    @Column(name = "detail")
    private String detail;

    protected PrivilegeAuditEntity() {
    }

    public PrivilegeAuditEntity(String eventType, String privilegeId, String customerId,
                                LocalDateTime occurredOn, String detail) {
        this.eventType = eventType;
        this.privilegeId = privilegeId;
        this.customerId = customerId;
        this.occurredOn = occurredOn;
        this.detail = detail;
    }

    public String getEventType()   { return eventType; }
    public String getPrivilegeId() { return privilegeId; }
    public String getCustomerId()  { return customerId; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
    public String getDetail()      { return detail; }
}
