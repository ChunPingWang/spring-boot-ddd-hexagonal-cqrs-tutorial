package com.bank.accountquery.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 核發優惠（事件溯源）的請求內文。
 */
public record GrantPrivilegeRequest(
    @NotBlank String type,
    @NotNull Integer totalQuota,
    @NotNull LocalDate validFrom,
    @NotNull LocalDate validTo
) {}
