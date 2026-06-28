package com.bank.accountquery.infrastructure.adapter.in.rest;

import com.bank.accountquery.domain.exception.AccountCurrencyMismatchException;
import com.bank.accountquery.domain.exception.AccountNotActiveException;
import com.bank.accountquery.domain.exception.AccountNotFoundException;
import com.bank.accountquery.domain.exception.AccountNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.InvalidAccountIdFormatException;
import com.bank.accountquery.domain.exception.PrivilegeNotFoundException;
import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.QueryRangeExceededException;
import com.bank.accountquery.domain.exception.UnsupportedCurrencyException;
import com.bank.accountquery.infrastructure.adapter.in.rest.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 將 Domain Exception 對應為 HTTP Status（見 API 設計規範 7.4）。
 * Controller / Domain 不需要知道 HTTP 狀態碼，由此處集中映射。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 403 授權失敗 ────────────────────────────────────────────────
    @ExceptionHandler(AccountNotOwnedByCustomerException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotOwned(AccountNotOwnedByCustomerException ex) {
        return build(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_OWNED_BY_CUSTOMER", ex.getMessage());
    }

    @ExceptionHandler(PrivilegeNotOwnedByCustomerException.class)
    public ResponseEntity<ApiResponse<Void>> handlePrivilegeNotOwned(PrivilegeNotOwnedByCustomerException ex) {
        return build(HttpStatus.FORBIDDEN, "PRIVILEGE_NOT_OWNED_BY_CUSTOMER", ex.getMessage());
    }

    // ── 404 資源不存在 ──────────────────────────────────────────────
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(PrivilegeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePrivilegeNotFound(PrivilegeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "PRIVILEGE_NOT_FOUND", ex.getMessage());
    }

    // ── 422 業務規則違反 ────────────────────────────────────────────
    @ExceptionHandler(QueryRangeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleQueryRangeExceeded(QueryRangeExceededException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "QUERY_RANGE_EXCEEDED", ex.getMessage());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotActive(AccountNotActiveException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_NOT_ACTIVE", ex.getMessage());
    }

    @ExceptionHandler(AccountCurrencyMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountCurrencyMismatch(AccountCurrencyMismatchException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_CURRENCY_MISMATCH", ex.getMessage());
    }

    // ── 400 請求參數錯誤 ────────────────────────────────────────────
    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedCurrency(UnsupportedCurrencyException ex) {
        return build(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CURRENCY", ex.getMessage());
    }

    @ExceptionHandler(InvalidAccountIdFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAccountId(InvalidAccountIdFormatException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_ID", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class,
                       MissingServletRequestParameterException.class,
                       MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    // ── 401 未認證（由 CurrentCustomerArgumentResolver 拋出）─────────
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        var status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = status == HttpStatus.UNAUTHORIZED ? "UNAUTHORIZED" : status.name();
        return build(status, code, ex.getReason());
    }

    // ── 500 系統錯誤 ────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系統發生未預期錯誤");
    }

    private static ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
