package com.bank.accountquery.infrastructure.adapter.out.persistence.jpa;

import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.account.AccountStatus;
import com.bank.accountquery.domain.model.account.AccountType;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.account.TransactionChannel;
import com.bank.accountquery.domain.model.account.TransactionId;
import com.bank.accountquery.domain.model.account.TransactionType;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.bank.accountquery.domain.model.privilege.PrivilegeUsageRecord;
import com.bank.accountquery.domain.model.privilege.TransferPrivilege;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.AccountEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.PrivilegeUsageRecordEntity;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.entity.TransactionEntity;
import java.util.List;

/**
 * Domain ↔ JPA Entity 映射。讓 Domain 維持純粹（不知道 JPA 的存在），
 * 由 Infrastructure 負責在邊界上轉換。
 */
public final class PersistenceMappers {

    private PersistenceMappers() {}

    // ── Account ─────────────────────────────────────────────────────
    public static Account toDomain(AccountEntity e) {
        return new Account(
            new AccountId(e.getAccountId()),
            CustomerId.of(e.getOwnerId()),
            AccountType.valueOf(e.getAccountType()),
            Currency.valueOf(e.getCurrency()),
            AccountStatus.valueOf(e.getStatus()));
    }

    public static AccountEntity toEntity(Account a) {
        return new AccountEntity(
            a.getAccountId().value(),
            a.getOwnerId().value(),
            a.getAccountType().name(),
            a.getCurrency().name(),
            a.getStatus().name());
    }

    // ── Transaction ─────────────────────────────────────────────────
    public static Transaction toDomain(TransactionEntity e) {
        return new Transaction(
            TransactionId.of(e.getTransactionId()),
            TransactionType.valueOf(e.getType()),
            Money.of(e.getAmount(), Currency.valueOf(e.getAmountCurrency())),
            e.getTwdEquivalent() == null ? null : Money.twd(e.getTwdEquivalent()),
            e.getTransactionDate(),
            e.getDescription(),
            TransactionChannel.valueOf(e.getChannel()));
    }

    public static TransactionEntity toEntity(Transaction t, String accountId) {
        return new TransactionEntity(
            t.getTransactionId().value(),
            accountId,
            t.getType().name(),
            t.getAmount().amount(),
            t.getAmount().currency().name(),
            t.getTwdEquivalent() == null ? null : t.getTwdEquivalent().amount(),
            t.transactionDate(),
            t.getDescription(),
            t.getChannel().name());
    }

    // ── Privilege（Aggregate）────────────────────────────────────────
    public static TransferPrivilege toDomain(PrivilegeEntity e) {
        List<PrivilegeUsageRecord> records = e.getUsageRecords().stream()
            .map(r -> new PrivilegeUsageRecord(r.getUsedDate(), Money.twd(r.getSavedAmount()), r.getTargetAccountNo()))
            .toList();
        return new TransferPrivilege(
            PrivilegeId.of(e.getPrivilegeId()),
            CustomerId.of(e.getOwnerId()),
            PrivilegeType.valueOf(e.getType()),
            e.getTotalQuota(),
            e.getUsedQuota(),
            new DateRange(e.getValidFrom(), e.getValidTo()),
            records);
    }

    /** 建立全新的 PrivilegeEntity（含 usage records）。 */
    public static PrivilegeEntity toNewEntity(TransferPrivilege p) {
        PrivilegeEntity e = new PrivilegeEntity(
            p.getPrivilegeId().value(),
            p.getOwnerId().value(),
            p.getType().name(),
            p.getTotalQuota(),
            p.getUsedQuota(),
            p.getValidPeriod().startDate(),
            p.getValidPeriod().endDate());
        e.replaceUsageRecords(toUsageEntities(p));
        return e;
    }

    public static List<PrivilegeUsageRecordEntity> toUsageEntities(TransferPrivilege p) {
        return p.getUsageRecords().stream()
            .map(r -> new PrivilegeUsageRecordEntity(r.usedDate(), r.savedAmount().amount(), r.targetAccountNo()))
            .toList();
    }
}
