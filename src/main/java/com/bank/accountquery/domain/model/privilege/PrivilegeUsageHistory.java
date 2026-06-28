package com.bank.accountquery.domain.model.privilege;

import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * PrivilegeUsageHistory — Value Object（查詢結果封裝，不可變）。
 */
public record PrivilegeUsageHistory(
    PrivilegeId privilegeId,
    List<PrivilegeUsageRecord> records,
    DateRange queriedRange
) {
    public PrivilegeUsageHistory {
        Objects.requireNonNull(privilegeId);
        records = List.copyOf(records);   // 防禦性複製
        Objects.requireNonNull(queriedRange);
    }

    public int count() {
        return records.size();
    }

    /**
     * 區間內總節省金額（以台幣計）。
     */
    public Money totalSaved() {
        return records.stream()
            .map(PrivilegeUsageRecord::savedAmount)
            .reduce(Money.twd(BigDecimal.ZERO), Money::add);
    }
}
