package com.bank.accountquery.domain.model.privilege;

import java.util.Objects;

public record PrivilegeId(String value) {

    public PrivilegeId {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("PrivilegeId 不可為空");
        }
    }

    public static PrivilegeId of(String value) {
        return new PrivilegeId(value);
    }
}
