package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
public class TransactionEntity {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "account_id", nullable = false, length = 14)
    private String accountId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_currency", nullable = false)
    private String amountCurrency;

    @Column(name = "twd_equivalent")
    private BigDecimal twdEquivalent;   // 外幣交易才有，台幣為 null

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "description")
    private String description;

    @Column(name = "channel", nullable = false)
    private String channel;

    protected TransactionEntity() {
    }

    public TransactionEntity(String transactionId, String accountId, String type, BigDecimal amount,
                             String amountCurrency, BigDecimal twdEquivalent, LocalDateTime transactionDate,
                             String description, String channel) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.amountCurrency = amountCurrency;
        this.twdEquivalent = twdEquivalent;
        this.transactionDate = transactionDate;
        this.description = description;
        this.channel = channel;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountId()     { return accountId; }
    public String getType()          { return type; }
    public BigDecimal getAmount()    { return amount; }
    public String getAmountCurrency() { return amountCurrency; }
    public BigDecimal getTwdEquivalent() { return twdEquivalent; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public String getDescription()   { return description; }
    public String getChannel()       { return channel; }
}
