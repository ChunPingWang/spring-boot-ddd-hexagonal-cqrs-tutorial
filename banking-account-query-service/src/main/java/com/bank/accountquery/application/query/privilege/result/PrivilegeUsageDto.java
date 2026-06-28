package com.bank.accountquery.application.query.privilege.result;

import com.bank.accountquery.domain.model.privilege.PrivilegeUsageRecord;

public record PrivilegeUsageDto(
    String usedDate,
    String savedAmount,
    String targetAccountNo
) {
    public static PrivilegeUsageDto from(PrivilegeUsageRecord r) {
        return new PrivilegeUsageDto(
            r.usedDate().toString(),
            r.savedAmount().amount().toPlainString(),
            r.targetAccountNo()
        );
    }
}
