package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.command.privilege.GrantTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;

/** Input Port（事件溯源）— 核發優惠。 */
public interface GrantTransferPrivilegeUseCase {
    UseTransferPrivilegeResult execute(GrantTransferPrivilegeCommand command);
}
