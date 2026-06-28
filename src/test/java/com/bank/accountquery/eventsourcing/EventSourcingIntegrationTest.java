package com.bank.accountquery.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.accountquery.application.command.privilege.GrantTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.UseTransferPrivilegeCommand;
import com.bank.accountquery.application.port.in.GetEventSourcedPrivilegeUseCase;
import com.bank.accountquery.application.port.in.GrantTransferPrivilegeUseCase;
import com.bank.accountquery.application.port.in.UseEventSourcedPrivilegeUseCase;
import com.bank.accountquery.application.port.out.EventStorePort;
import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.domain.event.TransferPrivilegeUsedEvent;
import com.bank.accountquery.domain.exception.ConcurrencyConflictException;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.infrastructure.adapter.out.audit.PrivilegeAuditRepository;
import com.bank.accountquery.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 事件溯源整合測試（真實 PostgreSQL，Testcontainers）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "app.demo-seed.enabled=false")
@Import(TestcontainersConfiguration.class)
class EventSourcingIntegrationTest {

    @Autowired GrantTransferPrivilegeUseCase grant;
    @Autowired UseEventSourcedPrivilegeUseCase use;
    @Autowired GetEventSourcedPrivilegeUseCase query;
    @Autowired EventStorePort eventStore;
    @Autowired LoadPrivilegePort loadPrivilegePort;   // 既有讀取側（投影目標）
    @Autowired PrivilegeAuditRepository auditRepository;   // 領域事件消費端寫入

    private GrantTransferPrivilegeCommand grantCmd(String id) {
        return new GrantTransferPrivilegeCommand(
            CustomerId.of("C001"), PrivilegeId.of(id), PrivilegeType.FEE_FREE_INTERBANK_TRANSFER,
            10, new DateRange(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1)));
    }

    private UseTransferPrivilegeCommand useCmd(String id) {
        return new UseTransferPrivilegeCommand(
            CustomerId.of("C001"), PrivilegeId.of(id), Money.twd(new BigDecimal("15")), "81234567890123");
    }

    @Test
    @DisplayName("核發 + 使用兩次後，事件流為 1 Granted + 2 Used，重播狀態正確")
    void grant_then_use_twice_builds_event_stream_and_replays() {
        grant.execute(grantCmd("E001"));
        use.execute(useCmd("E001"));
        use.execute(useCmd("E001"));

        var stream = eventStore.load("E001");
        assertThat(stream).hasSize(3);
        assertThat(stream.get(0).getClass().getSimpleName()).isEqualTo("TransferPrivilegeGrantedEvent");
        assertThat(stream).filteredOn(e -> e instanceof TransferPrivilegeUsedEvent).hasSize(2);

        var state = query.replayState(PrivilegeId.of("E001"));
        assertThat(state.usedQuota()).isEqualTo(2);
        assertThat(state.remainingQuota()).isEqualTo(8);
    }

    @Test
    @DisplayName("事件溯源寫入會投影回既有讀表，可被現有查詢側讀到")
    void writes_are_projected_to_read_model() {
        grant.execute(grantCmd("E002"));
        use.execute(useCmd("E002"));

        var projected = loadPrivilegePort.findByPrivilegeId(PrivilegeId.of("E002")).orElseThrow();
        assertThat(projected.getUsedQuota()).isEqualTo(1);
        assertThat(projected.getRemainingQuota()).isEqualTo(9);
        // 使用紀錄也一併投影
        var history = projected.filterUsageHistory(
            new DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
        assertThat(history.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("以過期的版本附加事件應拋出並行衝突")
    void stale_version_append_conflicts() {
        grant.execute(grantCmd("E003"));   // 版本變為 1

        var used = new TransferPrivilegeUsedEvent(
            PrivilegeId.of("E003"), CustomerId.of("C001"), Money.twd(new BigDecimal("15")),
            "81234567890123", LocalDate.now(), 9);

        // 用過期版本 0 嘗試附加 → 衝突
        assertThatThrownBy(() -> eventStore.append("E003", 0, List.of(used)))
            .isInstanceOf(ConcurrencyConflictException.class);
    }

    @Test
    @DisplayName("領域事件消費端（@EventListener）會把每個事件寫入稽核日誌")
    void domain_events_are_consumed_into_audit_log() {
        grant.execute(grantCmd("E004"));   // → GRANTED
        use.execute(useCmd("E004"));       // → USED

        assertThat(auditRepository.countByPrivilegeId("E004")).isEqualTo(2);
    }
}
