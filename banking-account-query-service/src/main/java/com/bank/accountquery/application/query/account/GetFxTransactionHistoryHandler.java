package com.bank.accountquery.application.query.account;

import com.bank.accountquery.application.port.in.GetFxTransactionHistoryUseCase;
import com.bank.accountquery.application.port.out.LoadAccountPort;
import com.bank.accountquery.application.port.out.LoadTransactionPort;
import com.bank.accountquery.application.query.account.result.FxTransactionHistoryResult;
import com.bank.accountquery.domain.exception.AccountNotFoundException;
import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.account.TransactionHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GetFxTransactionHistoryHandler implements GetFxTransactionHistoryUseCase {

    private final LoadAccountPort loadAccountPort;
    private final LoadTransactionPort loadTransactionPort;

    public GetFxTransactionHistoryHandler(LoadAccountPort loadAccountPort,
                                          LoadTransactionPort loadTransactionPort) {
        this.loadAccountPort = loadAccountPort;
        this.loadTransactionPort = loadTransactionPort;
    }

    @Override
    public FxTransactionHistoryResult execute(GetFxTransactionHistoryQuery query) {
        Account account = loadAccountPort.findByAccountId(query.accountId())
            .orElseThrow(() -> new AccountNotFoundException(query.accountId()));

        account.verifyOwnership(query.customerId());
        account.ensureActive();

        // 幣別符合性由 Account Domain Method 驗證（filterByDateRange 內部）
        List<Transaction> rawTransactions =
            loadTransactionPort.findByAccountId(query.accountId(), query.dateRange());

        TransactionHistory history =
            account.filterByDateRange(rawTransactions, query.dateRange());

        return FxTransactionHistoryResult.from(history, query.currency(),
                                               query.page(), query.size());
    }
}
