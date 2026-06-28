package com.bank.accountquery.application.port.out;

import com.bank.accountquery.domain.model.privilege.TransferPrivilege;

/**
 * Output Port（寫入側）— 以 Aggregate Root 為操作單位儲存優惠（見 ADR-001）。
 * 絕不提供 save(PrivilegeUsageRecord)，避免繞過 Aggregate 破壞不變式。
 */
public interface SavePrivilegePort {
    void save(TransferPrivilege privilege);
}
