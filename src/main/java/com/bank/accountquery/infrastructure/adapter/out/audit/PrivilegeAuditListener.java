package com.bank.accountquery.infrastructure.adapter.out.audit;

import com.bank.accountquery.domain.event.TransferPrivilegeGrantedEvent;
import com.bank.accountquery.domain.event.TransferPrivilegeUsedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 領域事件「消費端」範例：以 Spring {@code @EventListener} 解耦地監聽優惠事件，
 * 寫入稽核日誌。發布者（DomainEventPublisherAdapter）與此消費者互不知道對方存在。
 *
 * 註：此處為同步消費，示範 pub/sub 機制；正式版常用
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 或非同步佇列。
 */
@Component
public class PrivilegeAuditListener {

    private final PrivilegeAuditRepository repository;

    public PrivilegeAuditListener(PrivilegeAuditRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onGranted(TransferPrivilegeGrantedEvent event) {
        repository.save(new PrivilegeAuditEntity(
            "GRANTED", event.privilegeId().value(), event.ownerId().value(), event.occurredOn(),
            "核發 %d 次".formatted(event.totalQuota())));
    }

    @EventListener
    public void onUsed(TransferPrivilegeUsedEvent event) {
        repository.save(new PrivilegeAuditEntity(
            "USED", event.privilegeId().value(), event.customerId().value(), event.occurredOn(),
            "節省 %s、轉帳至 %s、剩餘 %d".formatted(
                event.savedAmount().amount().toPlainString(), event.targetAccountNo(), event.remainingQuota())));
    }
}
