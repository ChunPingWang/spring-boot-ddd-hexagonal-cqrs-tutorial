package com.bank.accountquery.application.command.privilege;

import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;
import com.bank.accountquery.application.port.in.GrantTransferPrivilegeUseCase;
import com.bank.accountquery.application.port.out.DomainEventPublisher;
import com.bank.accountquery.application.port.out.EventStorePort;
import com.bank.accountquery.application.port.out.SavePrivilegePort;
import com.bank.accountquery.domain.event.DomainEvent;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 事件溯源寫入：核發優惠。
 * 流程：建立聚合（產生 Granted 事件）→ append 到事件庫（版本 0）→ 投影回讀模型 → 發布事件。
 */
@Component
public class GrantTransferPrivilegeHandler implements GrantTransferPrivilegeUseCase {

    private final EventStorePort eventStore;
    private final SavePrivilegePort savePrivilegePort;
    private final DomainEventPublisher eventPublisher;

    public GrantTransferPrivilegeHandler(EventStorePort eventStore,
                                         SavePrivilegePort savePrivilegePort,
                                         DomainEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.savePrivilegePort = savePrivilegePort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UseTransferPrivilegeResult execute(GrantTransferPrivilegeCommand command) {
        TransferPrivilege privilege = TransferPrivilege.grant(
            command.privilegeId(), command.ownerId(), command.type(),
            command.totalQuota(), command.validPeriod());

        List<DomainEvent> events = privilege.pullDomainEvents();
        eventStore.append(command.privilegeId().value(), 0, events);  // 版本 0：新串流
        savePrivilegePort.save(privilege);                            // 投影回現有讀表
        events.forEach(eventPublisher::publish);
        return UseTransferPrivilegeResult.from(privilege);
    }
}
