package com.bank.accountquery.domain.model.privilege;

/**
 * 轉帳優惠類型。
 */
public enum PrivilegeType {
    FEE_FREE_INTERBANK_TRANSFER("免手續費跨行轉帳"),
    FEE_FREE_WIRE_TRANSFER("免手續費電匯");

    private final String description;

    PrivilegeType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
