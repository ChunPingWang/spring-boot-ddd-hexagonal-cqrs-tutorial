package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.query.privilege.GetPrivilegeUsageHistoryQuery;
import com.bank.accountquery.application.query.privilege.result.PrivilegeUsageHistoryResult;

public interface GetPrivilegeUsageHistoryUseCase {
    PrivilegeUsageHistoryResult execute(GetPrivilegeUsageHistoryQuery query);
}
