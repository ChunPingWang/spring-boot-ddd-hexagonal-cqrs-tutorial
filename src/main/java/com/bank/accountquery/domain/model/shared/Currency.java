package com.bank.accountquery.domain.model.shared;

import com.bank.accountquery.domain.exception.UnsupportedCurrencyException;

/**
 * 支援的幣別（ISO 4217 子集）。
 * 新增幣別只需擴充此 Enum（OCP — 開放封閉原則）。
 */
public enum Currency {
    TWD,
    USD,
    JPY,
    EUR;

    public static Currency fromCode(String code) {
        if (code == null) {
            throw new UnsupportedCurrencyException("null");
        }
        for (Currency c : values()) {
            if (c.name().equalsIgnoreCase(code)) {
                return c;
            }
        }
        throw new UnsupportedCurrencyException(code);
    }
}
