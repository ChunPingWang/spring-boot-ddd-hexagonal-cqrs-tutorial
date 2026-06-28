package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.shared.CustomerId;

public class PrivilegeNotOwnedByCustomerException extends RuntimeException {
    public PrivilegeNotOwnedByCustomerException(PrivilegeId privilegeId, CustomerId customerId) {
        super("優惠 [%s] 不屬於客戶 [%s]".formatted(privilegeId.value(), customerId.value()));
    }
}
