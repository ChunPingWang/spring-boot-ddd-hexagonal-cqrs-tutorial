package com.bank.accountquery.domain.model.shared;

import com.bank.accountquery.domain.exception.CurrencyMismatchException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Money — 不可變 Value Object，封裝金額與幣別的業務語意。
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("金額最多 2 位小數");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不可為負數");
        }
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public static Money twd(BigDecimal amount) {
        return new Money(amount, Currency.TWD);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
}
