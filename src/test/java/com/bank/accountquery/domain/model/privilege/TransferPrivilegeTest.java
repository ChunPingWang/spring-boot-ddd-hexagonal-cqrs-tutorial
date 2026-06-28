package com.bank.accountquery.domain.model.privilege;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.accountquery.domain.event.TransferPrivilegeUsedEvent;
import com.bank.accountquery.domain.exception.PrivilegeExpiredException;
import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.PrivilegeQuotaExhaustedException;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.fixture.PrivilegeTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransferPrivilegeTest {

    @Test
    @DisplayName("有效期內且有剩餘次數的優惠應回傳 isValid = true")
    void should_return_true_when_privilege_is_valid() {
        var privilege = PrivilegeTestFixture.activePrivilege(10, 3);

        assertThat(privilege.isValid()).isTrue();
        assertThat(privilege.getRemainingQuota()).isEqualTo(7);
    }

    @Test
    @DisplayName("已使用完次數的優惠應回傳 isValid = false")
    void should_return_false_when_quota_exhausted() {
        var privilege = PrivilegeTestFixture.activePrivilege(5, 5);

        assertThat(privilege.isValid()).isFalse();
        assertThat(privilege.getRemainingQuota()).isZero();
    }

    @Test
    @DisplayName("filterUsageHistory 應只回傳區間內的使用紀錄")
    void should_filter_usage_records_by_date_range() {
        var privilege = PrivilegeTestFixture.privilegeWithUsageRecords();
        var range = new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        var history = privilege.filterUsageHistory(range);

        assertThat(history.records()).isNotEmpty();
        assertThat(history.records()).allMatch(r -> range.contains(r.usedDate()));
    }

    @Test
    @DisplayName("非持有人查詢應拋出 PrivilegeNotOwnedByCustomerException")
    void should_throw_when_non_owner_verifies_ownership() {
        var privilege = PrivilegeTestFixture.activePrivilege(10, 0);

        assertThatThrownBy(() -> privilege.verifyOwnership(CustomerId.of("C999")))
            .isInstanceOf(PrivilegeNotOwnedByCustomerException.class);
    }

    // ── 寫入側：use() ─────────────────────────────────────────────────
    @Test
    @DisplayName("使用一次優惠應使已用次數 +1、剩餘次數 -1 並記錄領域事件")
    void should_consume_one_quota_and_record_event() {
        var privilege = PrivilegeTestFixture.activePrivilege(10, 3);

        privilege.use(PrivilegeTestFixture.DEFAULT_OWNER, Money.twd(new BigDecimal("15")), "81234567890123");

        assertThat(privilege.getUsedQuota()).isEqualTo(4);
        assertThat(privilege.getRemainingQuota()).isEqualTo(6);
        var events = privilege.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TransferPrivilegeUsedEvent.class);
    }

    @Test
    @DisplayName("使用後新增的紀錄應反映在 filterUsageHistory（命令影響查詢）")
    void should_reflect_new_usage_in_history() {
        var privilege = PrivilegeTestFixture.activePrivilege(10, 0);
        privilege.use(PrivilegeTestFixture.DEFAULT_OWNER, Money.twd(new BigDecimal("15")), "81234567890123");

        var range = new DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        assertThat(privilege.filterUsageHistory(range).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("pullDomainEvents 後再呼叫應為空（事件只發布一次）")
    void should_clear_events_after_pull() {
        var privilege = PrivilegeTestFixture.activePrivilege(10, 0);
        privilege.use(PrivilegeTestFixture.DEFAULT_OWNER, Money.twd(new BigDecimal("15")), "81234567890123");

        assertThat(privilege.pullDomainEvents()).hasSize(1);
        assertThat(privilege.pullDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("額度用盡時使用應拋出 PrivilegeQuotaExhaustedException")
    void should_throw_when_using_exhausted_privilege() {
        var privilege = PrivilegeTestFixture.activePrivilege(5, 5);

        assertThatThrownBy(() ->
            privilege.use(PrivilegeTestFixture.DEFAULT_OWNER, Money.twd(new BigDecimal("15")), "81234567890123"))
            .isInstanceOf(PrivilegeQuotaExhaustedException.class);
    }

    @Test
    @DisplayName("過期優惠使用應拋出 PrivilegeExpiredException")
    void should_throw_when_using_expired_privilege() {
        var privilege = PrivilegeTestFixture.expiredPrivilege(5, 0);

        assertThatThrownBy(() ->
            privilege.use(PrivilegeTestFixture.DEFAULT_OWNER, Money.twd(new BigDecimal("15")), "81234567890123"))
            .isInstanceOf(PrivilegeExpiredException.class);
    }

    @Test
    @DisplayName("非持有人使用優惠應拋出 PrivilegeNotOwnedByCustomerException")
    void should_throw_when_non_owner_uses_privilege() {
        var privilege = PrivilegeTestFixture.activePrivilege(10, 0);

        assertThatThrownBy(() ->
            privilege.use(CustomerId.of("C999"), Money.twd(new BigDecimal("15")), "81234567890123"))
            .isInstanceOf(PrivilegeNotOwnedByCustomerException.class);
    }

    @Test
    @DisplayName("持有人於有效期內使用優惠不應拋例外")
    void should_pass_when_owner_uses_valid_privilege() {
        var privilege = PrivilegeTestFixture.activePrivilege(10, 0);

        assertThatCode(() ->
            privilege.use(PrivilegeTestFixture.DEFAULT_OWNER, Money.twd(new BigDecimal("15")), "81234567890123"))
            .doesNotThrowAnyException();
    }
}
