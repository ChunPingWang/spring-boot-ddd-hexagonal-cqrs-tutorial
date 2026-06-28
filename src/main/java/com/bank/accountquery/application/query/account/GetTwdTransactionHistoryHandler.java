package com.bank.accountquery.application.query.account;

import com.bank.accountquery.application.port.in.GetTwdTransactionHistoryUseCase;
import com.bank.accountquery.application.port.out.LoadAccountPort;
import com.bank.accountquery.application.port.out.LoadTransactionPort;
import com.bank.accountquery.application.query.account.result.TwdTransactionHistoryResult;
import com.bank.accountquery.domain.exception.AccountNotFoundException;
import com.bank.accountquery.domain.model.account.Account;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.account.TransactionHistory;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Handler 只做流程協調：取得 Aggregate → 委派 Domain Method → 轉換 DTO。
 * 不含任何業務規則判斷（if/else 業務邏輯應在 Domain）。
 */
@Component
public class GetTwdTransactionHistoryHandler implements GetTwdTransactionHistoryUseCase {

    private final LoadAccountPort loadAccountPort;
    private final LoadTransactionPort loadTransactionPort;

    public GetTwdTransactionHistoryHandler(LoadAccountPort loadAccountPort,
                                           LoadTransactionPort loadTransactionPort) {
        this.loadAccountPort = loadAccountPort;
        this.loadTransactionPort = loadTransactionPort;
    }

    @Override
    public TwdTransactionHistoryResult execute(GetTwdTransactionHistoryQuery query) {
        // Step 1：透過 Output Port 取得 Aggregate
        Account account = loadAccountPort.findByAccountId(query.accountId())
            .orElseThrow(() -> new AccountNotFoundException(query.accountId()));

        // Step 2：委派業務規則至 Domain Model（所有權 + 帳戶狀態）
        account.verifyOwnership(query.customerId());
        account.ensureActive();

        // Step 3：透過 Output Port 取得原始交易資料
        List<Transaction> rawTransactions =
            loadTransactionPort.findByAccountId(query.accountId(), query.dateRange());

        // Step 4：委派業務規則至 Domain Model（查詢區間限制 + 過濾）
        TransactionHistory history =
            account.filterByDateRange(rawTransactions, query.dateRange());

        // Step 5：轉換為 Read Model
        return TwdTransactionHistoryResult.from(history, query.page(), query.size());
    }
}
