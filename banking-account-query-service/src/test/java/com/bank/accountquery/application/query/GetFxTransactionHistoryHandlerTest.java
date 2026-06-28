package com.bank.accountquery.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bank.accountquery.application.port.out.LoadAccountPort;
import com.bank.accountquery.application.port.out.LoadTransactionPort;
import com.bank.accountquery.application.query.account.GetFxTransactionHistoryHandler;
import com.bank.accountquery.domain.model.account.Transaction;
import com.bank.accountquery.domain.model.account.TransactionChannel;
import com.bank.accountquery.domain.model.account.TransactionId;
import com.bank.accountquery.domain.model.account.TransactionType;
import com.bank.accountquery.domain.model.shared.Currency;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.domain.model.shared.Money;
import com.bank.accountquery.fixture.AccountTestFixture;
import com.bank.accountquery.fixture.QueryFixture;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetFxTransactionHistoryHandlerTest {

    @Mock private LoadAccountPort loadAccountPort;
    @Mock private LoadTransactionPort loadTransactionPort;
    @InjectMocks private GetFxTransactionHistoryHandler handler;

    @Test
    @DisplayName("成功查詢美元交易紀錄並呈現原幣/台幣等值/匯率")
    void should_return_fx_transaction_history_with_twd_equivalent() {
        var query = QueryFixture.fxQuery("C001", "00123456789013", Currency.USD);
        var mockAccount = AccountTestFixture.activeFxAccount(CustomerId.of("C001"));
        var fxTx = new Transaction(
            TransactionId.of("F001"), TransactionType.CREDIT,
            Money.of(new BigDecimal("1000.00"), Currency.USD),
            Money.twd(new BigDecimal("32500.00")),
            LocalDateTime.of(2025, 1, 5, 10, 0), "美元購匯", TransactionChannel.ONLINE);

        given(loadAccountPort.findByAccountId(any())).willReturn(Optional.of(mockAccount));
        given(loadTransactionPort.findByAccountId(any(), any())).willReturn(List.of(fxTx));

        var result = handler.execute(query);

        assertThat(result.currencyCode()).isEqualTo("USD");
        assertThat(result.transactions()).hasSize(1);
        var dto = result.transactions().get(0);
        assertThat(dto.fxAmount()).isEqualTo("1000.00");
        assertThat(dto.twdEquivalent()).isEqualTo("32500.00");
        assertThat(dto.exchangeRate()).isEqualTo("32.5000");
    }
}
