package com.bank.accountquery.domain.exception;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;

public class PrivilegeQuotaExhaustedException extends RuntimeException {
    public PrivilegeQuotaExhaustedException(PrivilegeId privilegeId) {
        super("優惠 [%s] 可用次數已用罄，無法使用".formatted(privilegeId.value()));
    }
}
