package com.bank.accountquery.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.accountquery.application.port.in.GetFxTransactionHistoryUseCase;
import com.bank.accountquery.application.port.in.GetTwdTransactionHistoryUseCase;
import com.bank.accountquery.domain.exception.AccountNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.QueryRangeExceededException;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.fixture.TwdTransactionHistoryResultFixture;
import com.bank.accountquery.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GetTwdTransactionHistoryUseCase getTwdTransactionHistory;
    @MockitoBean GetFxTransactionHistoryUseCase getFxTransactionHistory;

    /** 模擬已認證客戶：注入一個 subject=customerId 的 JWT。 */
    private static JwtRequestPostProcessor asCustomer(String customerId) {
        return jwt().jwt(builder -> builder.subject(customerId));
    }

    @Test
    @DisplayName("成功查詢 — 回傳 200 與交易清單")
    void should_return_200_with_transactions() throws Exception {
        given(getTwdTransactionHistory.execute(any()))
            .willReturn(TwdTransactionHistoryResultFixture.sample());

        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .with(asCustomer("C001"))
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.transactions").isArray());
    }

    @Test
    @DisplayName("缺少 startDate — 回傳 400")
    void should_return_400_when_startDate_missing() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .with(asCustomer("C001"))
                .param("endDate", "2025-01-31"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("缺少 JWT — 回傳 401")
    void should_return_401_when_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("非帳戶持有人 — 回傳 403")
    void should_return_403_when_not_owner() throws Exception {
        given(getTwdTransactionHistory.execute(any()))
            .willThrow(new AccountNotOwnedByCustomerException(
                new AccountId("00123456789012"), CustomerId.of("C999")));

        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .with(asCustomer("C999"))
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_OWNED_BY_CUSTOMER"));
    }

    @Test
    @DisplayName("查詢區間超過 13 個月 — 回傳 422")
    void should_return_422_when_range_exceeded() throws Exception {
        given(getTwdTransactionHistory.execute(any()))
            .willThrow(new QueryRangeExceededException("查詢區間不可超過 13 個月"));

        mockMvc.perform(get("/api/v1/accounts/00123456789012/transactions/twd")
                .with(asCustomer("C001"))
                .param("startDate", "2023-12-01")
                .param("endDate", "2025-02-01"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("QUERY_RANGE_EXCEEDED"));
    }

    @Test
    @DisplayName("不支援的幣別 — 回傳 400 UNSUPPORTED_CURRENCY")
    void should_return_400_when_currency_unsupported() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/00123456789013/transactions/fx")
                .with(asCustomer("C001"))
                .param("currency", "XXX")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));
    }
}
