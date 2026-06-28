package com.bank.accountquery.application.query.privilege;

import com.bank.accountquery.application.port.in.GetTransferPrivilegeUseCase;
import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.query.privilege.result.TransferPrivilegeResult;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GetTransferPrivilegeHandler implements GetTransferPrivilegeUseCase {

    private final LoadPrivilegePort loadPrivilegePort;

    public GetTransferPrivilegeHandler(LoadPrivilegePort loadPrivilegePort) {
        this.loadPrivilegePort = loadPrivilegePort;
    }

    @Override
    public TransferPrivilegeResult execute(GetTransferPrivilegeQuery query) {
        List<TransferPrivilege> privileges =
            loadPrivilegePort.findByCustomerId(query.customerId());

        // 每個 Privilege Aggregate 自己計算業務狀態（isValid、getRemainingQuota）
        return TransferPrivilegeResult.from(privileges);
    }
}
