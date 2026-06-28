package com.bank.accountquery.domain.model.shared;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * DateRange — 封裝日期區間業務語意與驗證。
 */
public record DateRange(LocalDate startDate, LocalDate endDate) {

    public DateRange {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate 不可晚於 endDate");
        }
    }

    public boolean exceedsMonths(int months) {
        return ChronoUnit.MONTHS.between(startDate, endDate) > months;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
