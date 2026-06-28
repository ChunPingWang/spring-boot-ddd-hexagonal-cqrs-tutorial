package com.bank.accountquery.domain.model.shared;

import java.util.Objects;

/**
 * CustomerId — 強型別，防止原始型別濫用（Primitive Obsession）。
 */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("CustomerId 不可為空");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}
