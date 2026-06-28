package com.bank.accountquery.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bank.accountquery.application.command.privilege.UseTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.UseTransferPrivilegeHandler;
import com.bank.accountquery.application.port.out.DomainEventPublisher;
import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.port.out.SavePrivilegePort;
import com.bank.accountquery.domain.event.TransferPrivilegeUsedEvent;
import com.bank.accountquery.domain.exception.PrivilegeNotFoundException;
import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.fixture.PrivilegeTestFixture;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UseTransferPrivilegeHandlerTest {

    @Mock private LoadPrivilegePort loadPrivilegePort;
    @Mock private SavePrivilegePort savePrivilegePort;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private UseTransferPrivilegeHandler handler;

    private UseTransferPrivilegeCommand command(String customerId) {
        return new UseTransferPrivilegeCommand(
            CustomerId.of(customerId), PrivilegeId.of("P001"),
            Money.twd(new BigDecimal("15")), "81234567890123");
    }

    @Test
    @DisplayName("成功使用優惠：儲存整個 Aggregate、發布事件、回報剩餘次數")
    void should_use_privilege_save_aggregate_and_publish_event() {
        given(loadPrivilegePort.findByPrivilegeId(any()))
            .willReturn(Optional.of(PrivilegeTestFixture.activePrivilege(10, 3)));

        var result = handler.execute(command("C001"));

        assertThat(result.usedQuota()).isEqualTo(4);
        assertThat(result.remainingQuota()).isEqualTo(6);
        then(savePrivilegePort).should().save(any(TransferPrivilege.class));
        then(eventPublisher).should().publish(any(TransferPrivilegeUsedEvent.class));
    }

    @Test
    @DisplayName("優惠不存在應拋出 PrivilegeNotFoundException 且不儲存")
    void should_throw_when_privilege_not_found() {
        given(loadPrivilegePort.findByPrivilegeId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(command("C001")))
            .isInstanceOf(PrivilegeNotFoundException.class);

        then(savePrivilegePort).should(never()).save(any());
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("非持有人使用應拋例外，且不儲存、不發布事件")
    void should_throw_and_not_save_when_not_owner() {
        given(loadPrivilegePort.findByPrivilegeId(any()))
            .willReturn(Optional.of(PrivilegeTestFixture.activePrivilege(10, 3)));

        assertThatThrownBy(() -> handler.execute(command("C999")))
            .isInstanceOf(PrivilegeNotOwnedByCustomerException.class);

        then(savePrivilegePort).should(never()).save(any());
        then(eventPublisher).shouldHaveNoInteractions();
    }
}
