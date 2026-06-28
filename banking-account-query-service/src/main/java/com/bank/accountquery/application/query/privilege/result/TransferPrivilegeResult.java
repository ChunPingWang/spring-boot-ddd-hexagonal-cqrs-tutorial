package com.bank.accountquery.application.query.privilege.result;

import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import java.util.List;

public record TransferPrivilegeResult(
    List<TransferPrivilegeDto> privileges
) {
    public static TransferPrivilegeResult from(List<TransferPrivilege> privileges) {
        return new TransferPrivilegeResult(
            privileges.stream().map(TransferPrivilegeDto::from).toList()
        );
    }
}
