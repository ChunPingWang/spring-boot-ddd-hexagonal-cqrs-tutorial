package com.bank.accountquery.application.command.privilege;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.Money;

/**
 * Command 物件（不可變）— 表達一個改變狀態的意圖：使用一次轉帳優惠。
 */
public record UseTransferPrivilegeCommand(
    CustomerId customerId,
    PrivilegeId privilegeId,
    Money savedAmount,
    String targetAccountNo
) {}
