package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity — 對應 account 資料表。僅存在於 Infrastructure 層，與 Domain 的 Account 分離。
 */
@Entity
@Table(name = "account")
public class AccountEntity {

    @Id
    @Column(name = "account_id", length = 14)
    private String accountId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "status", nullable = false)
    private String status;

    protected AccountEntity() {
    }

    public AccountEntity(String accountId, String ownerId, String accountType, String currency, String status) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.accountType = accountType;
        this.currency = currency;
        this.status = status;
    }

    public String getAccountId() { return accountId; }
    public String getOwnerId()   { return ownerId; }
    public String getAccountType() { return accountType; }
    public String getCurrency()  { return currency; }
    public String getStatus()    { return status; }
}
