package com.bank.accountquery.infrastructure.adapter.out.persistence.inmemory;

import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.port.out.SavePrivilegePort;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.CustomerId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Driven Adapter — 實作 LoadPrivilegePort 與 SavePrivilegePort（記憶體版）。
 */
@Repository
public class InMemoryPrivilegeAdapter implements LoadPrivilegePort, SavePrivilegePort {

    private final InMemoryBankingDataStore store;

    public InMemoryPrivilegeAdapter(InMemoryBankingDataStore store) {
        this.store = store;
    }

    @Override
    public List<TransferPrivilege> findByCustomerId(CustomerId customerId) {
        return store.privileges().values().stream()
            .filter(p -> p.getOwnerId().equals(customerId))
            .toList();
    }

    @Override
    public Optional<TransferPrivilege> findByPrivilegeId(PrivilegeId privilegeId) {
        return Optional.ofNullable(store.privileges().get(privilegeId));
    }

    @Override
    public void save(TransferPrivilege privilege) {
        store.addPrivilege(privilege);
    }
}
