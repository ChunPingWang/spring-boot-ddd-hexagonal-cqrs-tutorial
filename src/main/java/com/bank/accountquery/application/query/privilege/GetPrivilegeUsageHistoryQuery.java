package com.bank.accountquery.application.query.privilege;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;

public record GetPrivilegeUsageHistoryQuery(
    CustomerId customerId,
    PrivilegeId privilegeId,
    DateRange dateRange,
    int page,
    int size
) {}
