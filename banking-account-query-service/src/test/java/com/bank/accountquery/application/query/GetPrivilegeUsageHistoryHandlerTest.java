package com.bank.accountquery.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.query.privilege.GetPrivilegeUsageHistoryHandler;
import com.bank.accountquery.application.query.privilege.GetPrivilegeUsageHistoryQuery;
import com.bank.accountquery.domain.exception.PrivilegeNotFoundException;
import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.fixture.PrivilegeTestFixture;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPrivilegeUsageHistoryHandlerTest {

    @Mock private LoadPrivilegePort loadPrivilegePort;
    @InjectMocks private GetPrivilegeUsageHistoryHandler handler;

    private GetPrivilegeUsageHistoryQuery query(String customerId) {
        return new GetPrivilegeUsageHistoryQuery(
            CustomerId.of(customerId),
            PrivilegeId.of("P001"),
            new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
            0, 20
        );
    }

    @Test
    @DisplayName("成功查詢優惠使用紀錄（僅回傳區間內，並由 Domain 計算總節省金額）")
    void should_return_usage_history_filtered_by_range() {
        given(loadPrivilegePort.findByPrivilegeId(any()))
            .willReturn(Optional.of(PrivilegeTestFixture.privilegeWithUsageRecords()));

        var result = handler.execute(query("C001"));

        assertThat(result.records()).hasSize(2);          // Jan 內 2 筆，Feb 1 筆被排除
        assertThat(result.totalSaved()).isEqualTo("60");
    }

    @Test
    @DisplayName("優惠不存在應拋出 PrivilegeNotFoundException")
    void should_throw_when_privilege_not_found() {
        given(loadPrivilegePort.findByPrivilegeId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(query("C001")))
            .isInstanceOf(PrivilegeNotFoundException.class);
    }

    @Test
    @DisplayName("非優惠持有人查詢應拋出 PrivilegeNotOwnedByCustomerException")
    void should_throw_when_not_owner() {
        given(loadPrivilegePort.findByPrivilegeId(any()))
            .willReturn(Optional.of(PrivilegeTestFixture.privilegeWithUsageRecords()));

        assertThatThrownBy(() -> handler.execute(query("C999")))
            .isInstanceOf(PrivilegeNotOwnedByCustomerException.class);
    }
}
