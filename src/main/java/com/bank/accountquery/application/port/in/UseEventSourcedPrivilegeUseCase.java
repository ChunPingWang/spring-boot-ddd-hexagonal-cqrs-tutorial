package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.command.privilege.UseTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;

/** Input Port（事件溯源）— 使用一次優惠（載入事件流 → 重播 → 附加事件）。 */
public interface UseEventSourcedPrivilegeUseCase {
    UseTransferPrivilegeResult execute(UseTransferPrivilegeCommand command);
}
