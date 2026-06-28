package com.bank.accountquery.application.port.out;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.CustomerId;
import java.util.List;
import java.util.Optional;

/**
 * Output Port — 取得轉帳優惠 Aggregate。
 * 可由 JPA Adapter 或 Cache Adapter（Decorator）實作（LSP）。
 */
public interface LoadPrivilegePort {
    List<TransferPrivilege> findByCustomerId(CustomerId customerId);
    Optional<TransferPrivilege> findByPrivilegeId(PrivilegeId privilegeId);
}
