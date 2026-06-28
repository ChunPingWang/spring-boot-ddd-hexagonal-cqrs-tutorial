package com.bank.accountquery.application.query.common;

import java.util.List;

/**
 * 分頁工具（Application Layer 的展示邏輯，非 Domain 業務規則）。
 */
public final class Pagination {

    private Pagination() {}

    public static <T> List<T> paginate(List<T> all, int page, int size) {
        if (size <= 0 || page < 0) {
            return List.of();
        }
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return List.copyOf(all.subList(from, to));
    }
}
