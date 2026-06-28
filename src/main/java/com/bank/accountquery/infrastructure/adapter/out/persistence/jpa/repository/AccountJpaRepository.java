package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository;

import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.AccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
    List<AccountEntity> findByOwnerId(String ownerId);
}
