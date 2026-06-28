package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa;

import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.port.out.SavePrivilegePort;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.PrivilegeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driven Adapter — 以 JPA/PostgreSQL 實作 LoadPrivilegePort 與 SavePrivilegePort。
 * 寫入以 Aggregate Root 為單位：一次保存優惠本身與其邊界內的使用紀錄（見 ADR-001）。
 */
@Repository
public class PrivilegePersistenceAdapter implements LoadPrivilegePort, SavePrivilegePort {

    private final PrivilegeJpaRepository repository;

    public PrivilegePersistenceAdapter(PrivilegeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferPrivilege> findByCustomerId(CustomerId customerId) {
        return repository.findByOwnerId(customerId.value()).stream()
            .map(PersistenceMappers::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransferPrivilege> findByPrivilegeId(PrivilegeId privilegeId) {
        return repository.findById(privilegeId.value()).map(PersistenceMappers::toDomain);
    }

    @Override
    @Transactional
    public void save(TransferPrivilege privilege) {
        PrivilegeEntity entity = repository.findById(privilege.getPrivilegeId().value()).orElse(null);
        if (entity == null) {
            entity = PersistenceMappers.toNewEntity(privilege);
        } else {
            entity.setUsedQuota(privilege.getUsedQuota());
            entity.replaceUsageRecords(PersistenceMappers.toUsageEntities(privilege));
        }
        repository.save(entity);
    }
}
