package com.bank.accountquery.application.query.common;

/**
 * 分頁資訊（展示需求，非業務規則）。
 */
public record PageInfo(int page, int size, int totalElements, int totalPages) {

    public static PageInfo of(int page, int size, int totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageInfo(page, size, totalElements, totalPages);
    }
}
