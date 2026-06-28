package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository;

import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeJpaRepository extends JpaRepository<PrivilegeEntity, String> {
    List<PrivilegeEntity> findByOwnerId(String ownerId);
}
