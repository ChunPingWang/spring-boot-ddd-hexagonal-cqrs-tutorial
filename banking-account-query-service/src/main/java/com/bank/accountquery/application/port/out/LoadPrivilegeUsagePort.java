package com.bank.accountquery.application.port.out;

import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeUsageRecord;
import com.bank.accountquery.domain.model.shared.DateRange;
import java.util.List;

/**
 * Output Port — 取得優惠使用紀錄（大量資料時的分離查詢，見 ADR-002 方案 A）。
 */
public interface LoadPrivilegeUsagePort {
    List<PrivilegeUsageRecord> findByPrivilegeId(PrivilegeId privilegeId, DateRange dateRange);
}
