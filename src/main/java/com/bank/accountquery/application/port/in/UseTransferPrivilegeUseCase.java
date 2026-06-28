package com.bank.accountquery.application.port.in;

import com.bank.accountquery.application.command.privilege.UseTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;

/**
 * Input Port（寫入側）— 使用一次轉帳優惠的 Use Case 契約。
 */
public interface UseTransferPrivilegeUseCase {
    UseTransferPrivilegeResult execute(UseTransferPrivilegeCommand command);
}
