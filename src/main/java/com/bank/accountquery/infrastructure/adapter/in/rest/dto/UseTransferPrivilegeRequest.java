package com.bank.accountquery.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 使用優惠的請求內文。
 */
public record UseTransferPrivilegeRequest(
    @NotBlank String targetAccountNo,
    @NotNull BigDecimal savedAmount
) {}
