package com.bank.accountquery.application.query.privilege;

import com.bank.accountquery.application.port.in.GetPrivilegeUsageHistoryUseCase;
import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.query.privilege.result.PrivilegeUsageHistoryResult;
import com.bank.accountquery.domain.exception.PrivilegeNotFoundException;
import com.bank.accountquery.domain.model.privilege.PrivilegeUsageHistory;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import org.springframework.stereotype.Component;

@Component
public class GetPrivilegeUsageHistoryHandler implements GetPrivilegeUsageHistoryUseCase {

    private final LoadPrivilegePort loadPrivilegePort;

    public GetPrivilegeUsageHistoryHandler(LoadPrivilegePort loadPrivilegePort) {
        this.loadPrivilegePort = loadPrivilegePort;
    }

    @Override
    public PrivilegeUsageHistoryResult execute(GetPrivilegeUsageHistoryQuery query) {
        TransferPrivilege privilege = loadPrivilegePort.findByPrivilegeId(query.privilegeId())
            .orElseThrow(() -> new PrivilegeNotFoundException(query.privilegeId()));

        // 委派所有權驗證至 Domain
        privilege.verifyOwnership(query.customerId());

        // 委派使用紀錄過濾至 Domain
        PrivilegeUsageHistory usageHistory =
            privilege.filterUsageHistory(query.dateRange());

        return PrivilegeUsageHistoryResult.from(usageHistory, query.page(), query.size());
    }
}
