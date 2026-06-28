package com.bank.accountquery.infrastructure.adapter.in.rest;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import com.bank.accountquery.application.port.in.GetFxTransactionHistoryUseCase;
import com.bank.accountquery.application.port.in.GetTwdTransactionHistoryUseCase;
import com.bank.accountquery.application.query.account.GetFxTransactionHistoryQuery;
import com.bank.accountquery.application.query.account.GetTwdTransactionHistoryQuery;
import com.bank.accountquery.application.query.account.result.FxTransactionHistoryResult;
import com.bank.accountquery.application.query.account.result.TwdTransactionHistoryResult;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.DateRange;
import com.bank.accountquery.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.bank.accountquery.infrastructure.adapter.in.rest.security.CurrentCustomer;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AccountController — Driving Adapter。
 * 職責：HTTP 轉換（Request → Query Object，Result → HTTP Response），不含業務邏輯。
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private static final int MAX_PAGE_SIZE = 100;

    private final GetTwdTransactionHistoryUseCase getTwdTransactionHistory;
    private final GetFxTransactionHistoryUseCase getFxTransactionHistory;

    public AccountController(GetTwdTransactionHistoryUseCase getTwdTransactionHistory,
                             GetFxTransactionHistoryUseCase getFxTransactionHistory) {
        this.getTwdTransactionHistory = getTwdTransactionHistory;
        this.getFxTransactionHistory = getFxTransactionHistory;
    }

    @GetMapping("/{accountId}/transactions/twd")
    public ResponseEntity<ApiResponse<TwdTransactionHistoryResult>> getTwdTransactions(
        @PathVariable String accountId,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @CurrentCustomer CustomerId customerId
    ) {
        var query = new GetTwdTransactionHistoryQuery(
            customerId,
            new AccountId(accountId),
            new DateRange(startDate, endDate),
            page, clampSize(size)
        );
        var result = getTwdTransactionHistory.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{accountId}/transactions/fx")
    public ResponseEntity<ApiResponse<FxTransactionHistoryResult>> getFxTransactions(
        @PathVariable String accountId,
        @RequestParam String currency,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @CurrentCustomer CustomerId customerId
    ) {
        var query = new GetFxTransactionHistoryQuery(
            customerId,
            new AccountId(accountId),
            Currency.fromCode(currency),
            new DateRange(startDate, endDate),
            page, clampSize(size)
        );
        var result = getFxTransactionHistory.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
