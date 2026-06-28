package com.bank.accountquery.application.query.privilege.result;

import com.bank.accountquery.application.query.common.PageInfo;
import com.bank.accountquery.application.query.common.Pagination;
import com.bank.accountquery.domain.model.privilege.PrivilegeUsageHistory;
import java.util.List;

public record PrivilegeUsageHistoryResult(
    String privilegeId,
    List<PrivilegeUsageDto> records,
    String totalSaved,
    PageInfo pageInfo
) {
    public static PrivilegeUsageHistoryResult from(PrivilegeUsageHistory history,
                                                   int page, int size) {
        var dtos = history.records().stream()
            .map(PrivilegeUsageDto::from)
            .toList();
        return new PrivilegeUsageHistoryResult(
            history.privilegeId().value(),
            Pagination.paginate(dtos, page, size),
            history.totalSaved().amount().toPlainString(),   // Domain 計算
            PageInfo.of(page, size, dtos.size())
        );
    }
}
