package com.bank.accountquery.domain.model.privilege;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.fixture.PrivilegeTestFixture;
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
}
