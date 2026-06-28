package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;

public class PrivilegeExpiredException extends RuntimeException {
    public PrivilegeExpiredException(PrivilegeId privilegeId) {
        super("優惠 [%s] 不在有效期間內，無法使用".formatted(privilegeId.value()));
    }
}
