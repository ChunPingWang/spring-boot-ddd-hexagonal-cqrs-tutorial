package com.bank.accountquery.infrastructure.adapter.in.rest;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import com.bank.accountquery.application.command.privilege.UseTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;
import com.bank.accountquery.application.port.in.GetPrivilegeUsageHistoryUseCase;
import com.bank.accountquery.application.port.in.GetTransferPrivilegeUseCase;
import com.bank.accountquery.application.port.in.UseTransferPrivilegeUseCase;
import com.bank.accountquery.application.query.privilege.GetPrivilegeUsageHistoryQuery;
import com.bank.accountquery.application.query.privilege.GetTransferPrivilegeQuery;
import com.bank.accountquery.application.query.privilege.result.PrivilegeUsageHistoryResult;
import com.bank.accountquery.application.query.privilege.result.TransferPrivilegeResult;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.bank.accountquery.infrastructure.adapter.in.rest.dto.UseTransferPrivilegeRequest;
import com.bank.accountquery.infrastructure.adapter.in.rest.security.CurrentCustomer;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PrivilegeController — Driving Adapter（轉帳優惠查詢與使用）。
 */
@RestController
@RequestMapping("/api/v1/customers/me/privileges/transfer")
public class PrivilegeController {

    private static final int MAX_PAGE_SIZE = 100;

    private final GetTransferPrivilegeUseCase getTransferPrivilege;
    private final GetPrivilegeUsageHistoryUseCase getPrivilegeUsageHistory;
    private final UseTransferPrivilegeUseCase useTransferPrivilege;

    public PrivilegeController(GetTransferPrivilegeUseCase getTransferPrivilege,
                               GetPrivilegeUsageHistoryUseCase getPrivilegeUsageHistory,
                               UseTransferPrivilegeUseCase useTransferPrivilege) {
        this.getTransferPrivilege = getTransferPrivilege;
        this.getPrivilegeUsageHistory = getPrivilegeUsageHistory;
        this.useTransferPrivilege = useTransferPrivilege;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TransferPrivilegeResult>> getTransferPrivileges(
        @CurrentCustomer CustomerId customerId
    ) {
        var query = new GetTransferPrivilegeQuery(customerId);
        var result = getTransferPrivilege.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{privilegeId}/usage")
    public ResponseEntity<ApiResponse<PrivilegeUsageHistoryResult>> getPrivilegeUsage(
        @PathVariable String privilegeId,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @CurrentCustomer CustomerId customerId
    ) {
        var query = new GetPrivilegeUsageHistoryQuery(
            customerId,
            PrivilegeId.of(privilegeId),
            new DateRange(startDate, endDate),
            page, clampSize(size)
        );
        var result = getPrivilegeUsageHistory.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── 寫入側：使用一次轉帳優惠（CQRS Command 側）──────────────────
    @PostMapping("/{privilegeId}/use")
    public ResponseEntity<ApiResponse<UseTransferPrivilegeResult>> usePrivilege(
        @PathVariable String privilegeId,
        @Valid @RequestBody UseTransferPrivilegeRequest request,
        @CurrentCustomer CustomerId customerId
    ) {
        var command = new UseTransferPrivilegeCommand(
            customerId,
            PrivilegeId.of(privilegeId),
            Money.twd(request.savedAmount()),
            request.targetAccountNo()
        );
        var result = useTransferPrivilege.execute(command);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
