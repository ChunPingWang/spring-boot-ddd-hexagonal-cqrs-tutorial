package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.query.privilege.GetTransferPrivilegeQuery;
import com.bank.accountquery.application.query.privilege.result.TransferPrivilegeResult;

public interface GetTransferPrivilegeUseCase {
    TransferPrivilegeResult execute(GetTransferPrivilegeQuery query);
}
