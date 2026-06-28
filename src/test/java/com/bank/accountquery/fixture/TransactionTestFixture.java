package com.bank.accountquery.fixture;

import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.account.TransactionChannel;
import com.bank.accountquery.domain.model.account.TransactionId;
import com.bank.accountquery.domain.model.account.TransactionType;
import com.bank.accountquery.domain.model.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TransactionTestFixture {

    private TransactionTestFixture() {}

    public static Transaction on(LocalDate date) {
        return new Transaction(
            TransactionId.of("TX-" + date),
            TransactionType.CREDIT,
            Money.twd(new BigDecimal("100.00")),
            null,
            date.atStartOfDay(),
            "測試交易",
            TransactionChannel.SYSTEM
        );
    }

    public static List<Transaction> sampleList() {
        return List.of(
            on(LocalDate.of(2025, 1, 5)),
            on(LocalDate.of(2025, 1, 10)),
            on(LocalDate.of(2025, 1, 20))
        );
    }
}
