package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;

public class PrivilegeNotFoundException extends RuntimeException {
    public PrivilegeNotFoundException(PrivilegeId privilegeId) {
        super("優惠 [%s] 不存在".formatted(privilegeId.value()));
    }
}
