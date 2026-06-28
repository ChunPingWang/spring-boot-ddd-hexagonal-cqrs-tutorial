package com.bank.accountquery.domain.model.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.accountquery.domain.exception.CurrencyMismatchException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    @DisplayName("相同幣別相加應回傳正確金額")
    void should_add_two_money_with_same_currency() {
        var m1 = Money.twd(new BigDecimal("1000.00"));
        var m2 = Money.twd(new BigDecimal("500.00"));

        var result = m1.add(m2);

        assertThat(result.amount()).isEqualByComparingTo("1500.00");
        assertThat(result.currency()).isEqualTo(Currency.TWD);
    }

    @Test
    @DisplayName("不同幣別相加應拋出 CurrencyMismatchException")
    void should_throw_when_adding_different_currencies() {
        var twd = Money.twd(new BigDecimal("1000"));
        var usd = new Money(new BigDecimal("30"), Currency.USD);

        assertThatThrownBy(() -> twd.add(usd))
            .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("負數金額應拋出 IllegalArgumentException")
    void should_throw_when_amount_is_negative() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), Currency.TWD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("金額不可為負數");
    }

    @Test
    @DisplayName("超過 2 位小數應拋出 IllegalArgumentException")
    void should_throw_when_scale_exceeds_two() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.234"), Currency.TWD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2 位小數");
    }
}
