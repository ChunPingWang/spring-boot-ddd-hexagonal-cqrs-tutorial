package com.bank.accountquery.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.accountquery.application.command.privilege.result.UseTransferPrivilegeResult;
import com.bank.accountquery.application.port.in.GetPrivilegeUsageHistoryUseCase;
import com.bank.accountquery.application.port.in.GetTransferPrivilegeUseCase;
import com.bank.accountquery.application.port.in.UseTransferPrivilegeUseCase;
import com.bank.accountquery.application.query.privilege.result.TransferPrivilegeDto;
import com.bank.accountquery.application.query.privilege.result.TransferPrivilegeResult;
import com.bank.accountquery.domain.exception.PrivilegeNotOwnedByCustomerException;
import com.bank.accountquery.domain.exception.PrivilegeQuotaExhaustedException;
import com.bank.accountquery.domain.model.privilege.PrivilegeId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.bank.accountquery.infrastructure.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PrivilegeController.class)
@Import(SecurityConfig.class)
class PrivilegeControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GetTransferPrivilegeUseCase getTransferPrivilege;
    @MockitoBean GetPrivilegeUsageHistoryUseCase getPrivilegeUsageHistory;
    @MockitoBean UseTransferPrivilegeUseCase useTransferPrivilege;

    private static JwtRequestPostProcessor asCustomer(String customerId) {
        return jwt().jwt(builder -> builder.subject(customerId));
    }

    @Test
    @DisplayName("成功查詢轉帳優惠 — 回傳 200 與優惠清單")
    void should_return_200_with_privileges() throws Exception {
        var dto = new TransferPrivilegeDto("P001", "FEE_FREE_INTERBANK_TRANSFER", "免手續費跨行轉帳",
            10, 3, 7, "2025-01-01", "2030-12-31", true, false);
        given(getTransferPrivilege.execute(any()))
            .willReturn(new TransferPrivilegeResult(List.of(dto)));

        mockMvc.perform(get("/api/v1/customers/me/privileges/transfer")
                .with(asCustomer("C001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.privileges[0].remainingQuota").value(7))
            .andExpect(jsonPath("$.data.privileges[0].isValid").value(true));
    }

    @Test
    @DisplayName("查詢不屬於自己的優惠使用紀錄 — 回傳 403")
    void should_return_403_when_not_owner() throws Exception {
        given(getPrivilegeUsageHistory.execute(any()))
            .willThrow(new PrivilegeNotOwnedByCustomerException(
                PrivilegeId.of("P999"), CustomerId.of("C001")));

        mockMvc.perform(get("/api/v1/customers/me/privileges/transfer/P999/usage")
                .with(asCustomer("C001"))
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PRIVILEGE_NOT_OWNED_BY_CUSTOMER"));
    }

    @Test
    @DisplayName("成功使用優惠 — 回傳 200 與使用後剩餘次數")
    void should_return_200_when_use_privilege() throws Exception {
        given(useTransferPrivilege.execute(any()))
            .willReturn(new UseTransferPrivilegeResult("P001", 4, 6));

        mockMvc.perform(post("/api/v1/customers/me/privileges/transfer/P001/use")
                .with(asCustomer("C001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetAccountNo\":\"81234567890123\",\"savedAmount\":15}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.remainingQuota").value(6));
    }

    @Test
    @DisplayName("額度用盡時使用優惠 — 回傳 422")
    void should_return_422_when_quota_exhausted() throws Exception {
        given(useTransferPrivilege.execute(any()))
            .willThrow(new PrivilegeQuotaExhaustedException(PrivilegeId.of("P001")));

        mockMvc.perform(post("/api/v1/customers/me/privileges/transfer/P001/use")
                .with(asCustomer("C001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetAccountNo\":\"81234567890123\",\"savedAmount\":15}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PRIVILEGE_QUOTA_EXHAUSTED"));
    }
}
