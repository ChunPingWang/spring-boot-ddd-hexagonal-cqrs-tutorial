package com.bank.accountquery.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.query.privilege.GetTransferPrivilegeHandler;
import com.bank.accountquery.application.query.privilege.GetTransferPrivilegeQuery;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.fixture.PrivilegeTestFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetTransferPrivilegeHandlerTest {

    @Mock private LoadPrivilegePort loadPrivilegePort;
    @InjectMocks private GetTransferPrivilegeHandler handler;

    @Test
    @DisplayName("查詢優惠應由 Aggregate 計算剩餘次數與有效狀態")
    void should_map_privilege_business_state_from_aggregate() {
        given(loadPrivilegePort.findByCustomerId(any()))
            .willReturn(List.of(PrivilegeTestFixture.activePrivilege(10, 3)));

        var result = handler.execute(new GetTransferPrivilegeQuery(CustomerId.of("C001")));

        assertThat(result.privileges()).hasSize(1);
        var dto = result.privileges().get(0);
        assertThat(dto.remainingQuota()).isEqualTo(7);
        assertThat(dto.isValid()).isTrue();
    }
}
