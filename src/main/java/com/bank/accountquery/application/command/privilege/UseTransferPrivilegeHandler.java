package com.bank.accountquery.application.command.privilege;

import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;
import com.bank.accountquery.application.port.in.UseTransferPrivilegeUseCase;
import com.bank.accountquery.application.port.out.DomainEventPublisher;
import com.bank.accountquery.application.port.out.LoadPrivilegePort;
import com.bank.accountquery.application.port.out.SavePrivilegePort;
import com.bank.accountquery.domain.exception.PrivilegeNotFoundException;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import org.springframework.stereotype.Component;

/**
 * 寫入側 Command Handler。仍只做協調：
 * 取得 Aggregate → 委派 Domain 行為（含不變式守護）→ 儲存整個 Aggregate Root → 發布領域事件。
 * 沒有任何業務判斷（if/else 規則都在 TransferPrivilege.use() 內）。
 */
@Component
public class UseTransferPrivilegeHandler implements UseTransferPrivilegeUseCase {

    private final LoadPrivilegePort loadPrivilegePort;
    private final SavePrivilegePort savePrivilegePort;
    private final DomainEventPublisher eventPublisher;

    public UseTransferPrivilegeHandler(LoadPrivilegePort loadPrivilegePort,
                                       SavePrivilegePort savePrivilegePort,
                                       DomainEventPublisher eventPublisher) {
        this.loadPrivilegePort = loadPrivilegePort;
        this.savePrivilegePort = savePrivilegePort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UseTransferPrivilegeResult execute(UseTransferPrivilegeCommand command) {
        // Step 1：取得 Aggregate
        TransferPrivilege privilege = loadPrivilegePort.findByPrivilegeId(command.privilegeId())
            .orElseThrow(() -> new PrivilegeNotFoundException(command.privilegeId()));

        // Step 2：委派寫入行為至 Domain（守護所有權、有效期、剩餘額度等不變式）
        privilege.use(command.customerId(), command.savedAmount(), command.targetAccountNo());

        // Step 3：以 Aggregate Root 為單位儲存（見 ADR-001）
        savePrivilegePort.save(privilege);

        // Step 4：發布領域事件（儲存成功後）
        privilege.pullDomainEvents().forEach(eventPublisher::publish);

        // Step 5：回傳使用後狀態
        return UseTransferPrivilegeResult.from(privilege);
    }
}
