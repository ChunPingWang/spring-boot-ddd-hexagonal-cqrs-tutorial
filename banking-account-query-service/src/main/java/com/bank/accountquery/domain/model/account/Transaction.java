package com.bank.accountquery.domain.model.account;

import com.bank.accountquery.domain.model.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transaction — Entity（屬於 Account Aggregate 邊界內）。
 * 不對外建立獨立 Repository（見 ADR-001 / ADR-002 規則 3）。
 */
public class Transaction {

    private final TransactionId transactionId;
    private final TransactionType type;
    private final Money amount;            // 原幣金額
    private final Money twdEquivalent;     // 台幣等值（外幣帳戶才有，TWD 帳戶為 null）
    private final LocalDateTime transactionDate;
    private final String description;
    private final TransactionChannel channel;

    public Transaction(TransactionId transactionId,
                       TransactionType type,
                       Money amount,
                       Money twdEquivalent,
                       LocalDateTime transactionDate,
                       String description,
                       TransactionChannel channel) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.twdEquivalent = twdEquivalent;
        this.transactionDate = Objects.requireNonNull(transactionDate);
        this.description = description == null ? "" : description;
        this.channel = Objects.requireNonNull(channel);
    }

    /**
     * 外幣交易匯率 = 台幣等值 / 原幣金額（原幣金額為 0 時回傳 null）。
     */
    public BigDecimal exchangeRate() {
        if (twdEquivalent == null || amount.amount().signum() == 0) {
            return null;
        }
        return twdEquivalent.amount().divide(amount.amount(), 4, java.math.RoundingMode.HALF_UP);
    }

    public TransactionId getTransactionId() { return transactionId; }
    public TransactionType getType()        { return type; }
    public Money getAmount()                { return amount; }
    public Money getTwdEquivalent()         { return twdEquivalent; }
    public LocalDateTime transactionDate()  { return transactionDate; }
    public String getDescription()          { return description; }
    public TransactionChannel getChannel()  { return channel; }
}
