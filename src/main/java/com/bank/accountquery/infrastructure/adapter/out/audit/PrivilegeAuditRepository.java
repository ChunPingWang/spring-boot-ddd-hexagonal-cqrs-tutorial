package com.bank.accountquery.infrastructure.adapter.out.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeAuditRepository extends JpaRepository<PrivilegeAuditEntity, Long> {
    long countByPrivilegeId(String privilegeId);
}
