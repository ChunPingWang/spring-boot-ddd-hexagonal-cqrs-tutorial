package com.bank.accountquery.application.query.privilege;

import com.bank.accountquery.application.command.privilege.result.EventSourcedPrivilegeState;
import com.bank.accountquery.application.command.privilege.result.PrivilegeEventView;
import com.bank.accountquery.application.port.in.GetEventSourcedPrivilegeUseCase;
import com.bank.accountquery.application.port.out.EventStorePort;
import com.bank.accountquery.domain.event.DomainEvent;
import com.bank.accountquery.domain.exception.PrivilegeNotFoundException;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 事件溯源讀取：直接從事件庫重播重建狀態，或回傳原始事件序列。
 * 示範「狀態是事件折疊的結果」，與投影到讀表的查詢互補。
 */
@Component
public class GetEventSourcedPrivilegeHandler implements GetEventSourcedPrivilegeUseCase {

    private final EventStorePort eventStore;

    public GetEventSourcedPrivilegeHandler(EventStorePort eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public EventSourcedPrivilegeState replayState(PrivilegeId privilegeId) {
        List<DomainEvent> stream = loadOrThrow(privilegeId);
        return EventSourcedPrivilegeState.from(TransferPrivilege.rehydrate(stream));
    }

    @Override
    public List<PrivilegeEventView> eventLog(PrivilegeId privilegeId) {
        List<DomainEvent> stream = loadOrThrow(privilegeId);
        return java.util.stream.IntStream.range(0, stream.size())
            .mapToObj(i -> PrivilegeEventView.from(i + 1, stream.get(i)))
            .toList();
    }

    private List<DomainEvent> loadOrThrow(PrivilegeId privilegeId) {
        List<DomainEvent> stream = eventStore.load(privilegeId.value());
        if (stream.isEmpty()) {
            throw new PrivilegeNotFoundException(privilegeId);
        }
        return stream;
    }
}
