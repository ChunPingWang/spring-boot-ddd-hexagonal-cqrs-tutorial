package com.bank.accountquery.application.command.privilege;

import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;
import com.bank.accountquery.application.port.in.UseEventSourcedPrivilegeUseCase;
import com.bank.accountquery.application.port.out.DomainEventPublisher;
import com.bank.accountquery.application.port.out.EventStorePort;
import com.bank.accountquery.application.port.out.SavePrivilegePort;
import com.bank.accountquery.domain.event.DomainEvent;
import com.bank.accountquery.domain.exception.PrivilegeNotFoundException;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 事件溯源寫入：使用一次優惠。
 * 流程：載入事件流 → 重播重建聚合 → use()（守護不變式並產生 Used 事件）
 *      → append（以串流長度做樂觀並行控制）→ 投影回讀模型 → 發布事件。
 */
@Component
public class UseEventSourcedPrivilegeHandler implements UseEventSourcedPrivilegeUseCase {

    private final EventStorePort eventStore;
    private final SavePrivilegePort savePrivilegePort;
    private final DomainEventPublisher eventPublisher;

    public UseEventSourcedPrivilegeHandler(EventStorePort eventStore,
                                           SavePrivilegePort savePrivilegePort,
                                           DomainEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.savePrivilegePort = savePrivilegePort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UseTransferPrivilegeResult execute(UseTransferPrivilegeCommand command) {
        String aggregateId = command.privilegeId().value();
        List<DomainEvent> stream = eventStore.load(aggregateId);
        if (stream.isEmpty()) {
            throw new PrivilegeNotFoundException(command.privilegeId());
        }

        TransferPrivilege privilege = TransferPrivilege.rehydrate(stream);
        privilege.use(command.customerId(), command.savedAmount(), command.targetAccountNo());

        List<DomainEvent> newEvents = privilege.pullDomainEvents();
        eventStore.append(aggregateId, stream.size(), newEvents);
        savePrivilegePort.save(privilege);
        newEvents.forEach(eventPublisher::publish);
        return UseTransferPrivilegeResult.from(privilege);
    }
}
