package com.bank.accountquery.domain.model.privilege;

import com.bank.accountquery.domain.model.shared.Money;
import java.time.LocalDate;
import java.util.Objects;

/**
 * PrivilegeUsageRecord — Entity（屬於 TransferPrivilege Aggregate 邊界內）。
 */
public record PrivilegeUsageRecord(
    LocalDate usedDate,
    Money savedAmount,
    String targetAccountNo
) {
    public PrivilegeUsageRecord {
        Objects.requireNonNull(usedDate, "usedDate must not be null");
        Objects.requireNonNull(savedAmount, "savedAmount must not be null");
        Objects.requireNonNull(targetAccountNo, "targetAccountNo must not be null");
    }
}
