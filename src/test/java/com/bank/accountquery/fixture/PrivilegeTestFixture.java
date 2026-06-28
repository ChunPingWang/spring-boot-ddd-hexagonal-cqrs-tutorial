package com.bank.accountquery.fixture;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.privilege.PrivilegeUsageRecord;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PrivilegeTestFixture {

    public static final CustomerId DEFAULT_OWNER = CustomerId.of("C001");

    private PrivilegeTestFixture() {}

    /** 有效期涵蓋「今天」的優惠（以相對日期建立，避免測試隨時間失效）。 */
    public static TransferPrivilege activePrivilege(int totalQuota, int usedQuota) {
        return new TransferPrivilege(
            PrivilegeId.of("P001"),
            DEFAULT_OWNER,
            PrivilegeType.FEE_FREE_INTERBANK_TRANSFER,
            totalQuota,
            usedQuota,
            new DateRange(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1)),
            List.of()
        );
    }

    public static TransferPrivilege privilegeWithUsageRecords() {
        return new TransferPrivilege(
            PrivilegeId.of("P001"),
            DEFAULT_OWNER,
            PrivilegeType.FEE_FREE_INTERBANK_TRANSFER,
            10, 3,
            new DateRange(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1)),
            List.of(
                new PrivilegeUsageRecord(LocalDate.of(2025, 1, 5), Money.twd(new BigDecimal("30")), "81234567890123"),
                new PrivilegeUsageRecord(LocalDate.of(2025, 1, 12), Money.twd(new BigDecimal("30")), "91111222333444"),
                new PrivilegeUsageRecord(LocalDate.of(2025, 2, 1), Money.twd(new BigDecimal("30")), "11112222333344")
            )
        );
    }
}
