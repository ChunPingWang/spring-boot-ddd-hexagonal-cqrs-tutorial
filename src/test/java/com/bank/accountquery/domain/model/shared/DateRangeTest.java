package com.bank.accountquery.domain.model.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    @Test
    @DisplayName("查詢區間超過 13 個月應回傳 true")
    void should_return_true_when_range_exceeds_13_months() {
        var range = new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 3, 1));

        assertThat(range.exceedsMonths(13)).isTrue();
    }

    @Test
    @DisplayName("查詢區間未超過 13 個月應回傳 false")
    void should_return_false_when_range_within_13_months() {
        var range = new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 1));

        assertThat(range.exceedsMonths(13)).isFalse();
    }

    @Test
    @DisplayName("startDate 晚於 endDate 應拋出例外")
    void should_throw_when_start_is_after_end() {
        assertThatThrownBy(() ->
            new DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 1, 1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("contains 應正確判斷日期是否落在區間內（含邊界）")
    void should_check_contains() {
        var range = new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertThat(range.contains(LocalDate.of(2025, 1, 1))).isTrue();
        assertThat(range.contains(LocalDate.of(2025, 1, 31))).isTrue();
        assertThat(range.contains(LocalDate.of(2025, 2, 1))).isFalse();
    }
}
