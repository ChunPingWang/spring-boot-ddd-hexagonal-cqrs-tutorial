package com.bank.accountquery.application.command.privilege;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;

/**
 * Command（事件溯源）— 核發一個新的轉帳優惠（建立事件流）。
 */
public record GrantTransferPrivilegeCommand(
    CustomerId ownerId,
    PrivilegeId privilegeId,
    PrivilegeType type,
    int totalQuota,
    DateRange validPeriod
) {}
