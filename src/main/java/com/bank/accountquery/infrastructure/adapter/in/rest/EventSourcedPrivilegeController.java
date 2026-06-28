package com.bank.accountquery.infrastructure.adapter.in.rest;

import com.bank.accountquery.application.command.privilege.GrantTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.UseTransferPrivilegeCommand;
import com.bank.accountquery.application.command.privilege.result.EventSourcedPrivilegeState;
import com.bank.accountquery.application.command.privilege.result.PrivilegeEventView;
import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;
import com.bank.accountquery.application.port.in.GetEventSourcedPrivilegeUseCase;
import com.bank.accountquery.application.port.in.GrantTransferPrivilegeUseCase;
import com.bank.accountquery.application.port.in.UseEventSourcedPrivilegeUseCase;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.bank.accountquery.infrastructure.adapter.in.rest.dto.GrantPrivilegeRequest;
import com.bank.accountquery.infrastructure.adapter.in.rest.dto.UseTransferPrivilegeRequest;
import com.bank.accountquery.infrastructure.adapter.in.rest.security.CurrentCustomer;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事件溯源範例的 Driving Adapter（與既有狀態儲存的優惠端點並存對照）。
 * grant / use 走事件庫；GET 狀態由事件重播得到；GET events 顯示原始事件序列。
 */
@RestController
@RequestMapping("/api/v1/es/privileges")
public class EventSourcedPrivilegeController {

    private final GrantTransferPrivilegeUseCase grantPrivilege;
    private final UseEventSourcedPrivilegeUseCase usePrivilege;
    private final GetEventSourcedPrivilegeUseCase getPrivilege;

    public EventSourcedPrivilegeController(GrantTransferPrivilegeUseCase grantPrivilege,
                                           UseEventSourcedPrivilegeUseCase usePrivilege,
                                           GetEventSourcedPrivilegeUseCase getPrivilege) {
        this.grantPrivilege = grantPrivilege;
        this.usePrivilege = usePrivilege;
        this.getPrivilege = getPrivilege;
    }

    @PostMapping("/{privilegeId}/grant")
    public ResponseEntity<ApiResponse<UseTransferPrivilegeResult>> grant(
        @PathVariable String privilegeId,
        @Valid @RequestBody GrantPrivilegeRequest request,
        @CurrentCustomer CustomerId customerId
    ) {
        var command = new GrantTransferPrivilegeCommand(
            customerId,
            PrivilegeId.of(privilegeId),
            PrivilegeType.valueOf(request.type()),
            request.totalQuota(),
            new DateRange(request.validFrom(), request.validTo()));
        return ResponseEntity.ok(ApiResponse.success(grantPrivilege.execute(command)));
    }

    @PostMapping("/{privilegeId}/use")
    public ResponseEntity<ApiResponse<UseTransferPrivilegeResult>> use(
        @PathVariable String privilegeId,
        @Valid @RequestBody UseTransferPrivilegeRequest request,
        @CurrentCustomer CustomerId customerId
    ) {
        var command = new UseTransferPrivilegeCommand(
            customerId,
            PrivilegeId.of(privilegeId),
            Money.twd(request.savedAmount()),
            request.targetAccountNo());
        return ResponseEntity.ok(ApiResponse.success(usePrivilege.execute(command)));
    }

    @GetMapping("/{privilegeId}")
    public ResponseEntity<ApiResponse<EventSourcedPrivilegeState>> replayState(
        @PathVariable String privilegeId,
        @CurrentCustomer CustomerId customerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(getPrivilege.replayState(PrivilegeId.of(privilegeId))));
    }

    @GetMapping("/{privilegeId}/events")
    public ResponseEntity<ApiResponse<List<PrivilegeEventView>>> events(
        @PathVariable String privilegeId,
        @CurrentCustomer CustomerId customerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(getPrivilege.eventLog(PrivilegeId.of(privilegeId))));
    }
}
