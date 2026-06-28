package com.bank.accountquery.domain.model.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.accountquery.domain.exception.AccountNotActiveException;
import com.bank.accountquery.domain.exception.AccountNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.QueryRangeExceededException;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.fixture.AccountTestFixture;
import com.bank.accountquery.fixture.TransactionTestFixture;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    @DisplayName("帳戶持有人呼叫 verifyOwnership 應通過")
    void should_pass_when_owner_verifies_ownership() {
        var ownerId = CustomerId.of("C001");
        var account = AccountTestFixture.activeTwdAccount(ownerId);

        assertThatCode(() -> account.verifyOwnership(ownerId))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("非持有人呼叫 verifyOwnership 應拋出 AccountNotOwnedByCustomerException")
    void should_throw_when_non_owner_verifies_ownership() {
        var owner = CustomerId.of("C001");
        var intruder = CustomerId.of("C999");
        var account = AccountTestFixture.activeTwdAccount(owner);

        assertThatThrownBy(() -> account.verifyOwnership(intruder))
            .isInstanceOf(AccountNotOwnedByCustomerException.class);
    }

    @Test
    @DisplayName("凍結帳戶呼叫 ensureActive 應拋出 AccountNotActiveException")
    void should_throw_when_account_is_frozen() {
        var account = AccountTestFixture.frozenTwdAccount(CustomerId.of("C001"));

        assertThatThrownBy(account::ensureActive)
            .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    @DisplayName("filterByDateRange 超過 13 個月應拋出 QueryRangeExceededException")
    void should_throw_when_query_range_exceeds_13_months() {
        var account = AccountTestFixture.activeTwdAccount();
        List<Transaction> transactions = Collections.emptyList();
        var invalidRange = new DateRange(LocalDate.now().minusMonths(14), LocalDate.now());

        assertThatThrownBy(() -> account.filterByDateRange(transactions, invalidRange))
            .isInstanceOf(QueryRangeExceededException.class)
            .hasMessageContaining("13 個月");
    }

    @Test
    @DisplayName("filterByDateRange 應只回傳區間內的交易")
    void should_return_only_transactions_within_date_range() {
        var account = AccountTestFixture.activeTwdAccount();
        var range = new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        var transactions = List.of(
            TransactionTestFixture.on(LocalDate.of(2025, 1, 10)),
            TransactionTestFixture.on(LocalDate.of(2025, 2, 5))  // 區間外
        );

        var history = account.filterByDateRange(transactions, range);

        assertThat(history.transactions()).hasSize(1);
        assertThat(history.transactions().get(0).transactionDate().toLocalDate())
            .isEqualTo(LocalDate.of(2025, 1, 10));
    }
}
