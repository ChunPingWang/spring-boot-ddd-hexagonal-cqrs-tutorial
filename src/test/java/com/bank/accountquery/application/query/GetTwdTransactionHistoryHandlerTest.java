package com.bank.accountquery.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bank.accountquery.application.port.out.LoadAccountPort;
import com.bank.accountquery.application.port.out.LoadTransactionPort;
import com.bank.accountquery.application.query.account.GetTwdTransactionHistoryHandler;
import com.bank.accountquery.domain.exception.AccountNotFoundException;
import com.bank.accountquery.domain.exception.AccountNotOwnedByCustomerException;
import com.bank.accountquery.domain.model.account.AccountId;
import com.bank.accountquery.domain.model.shared.CustomerId;
import com.bank.accountquery.fixture.AccountTestFixture;
import com.bank.accountquery.fixture.QueryFixture;
import com.bank.accountquery.fixture.TransactionTestFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetTwdTransactionHistoryHandlerTest {

    @Mock private LoadAccountPort loadAccountPort;
    @Mock private LoadTransactionPort loadTransactionPort;
    @InjectMocks private GetTwdTransactionHistoryHandler handler;

    @Test
    @DisplayName("成功查詢台幣交易紀錄")
    void should_return_twd_transaction_history_successfully() {
        var query = QueryFixture.twdQuery("C001", "00123456789012");
        var mockAccount = AccountTestFixture.activeTwdAccount(CustomerId.of("C001"));
        var mockTransactions = TransactionTestFixture.sampleList();

        given(loadAccountPort.findByAccountId(any())).willReturn(Optional.of(mockAccount));
        given(loadTransactionPort.findByAccountId(any(), any())).willReturn(mockTransactions);

        var result = handler.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.transactions()).isNotEmpty();
        then(loadAccountPort).should().findByAccountId(new AccountId("00123456789012"));
        then(loadTransactionPort).should().findByAccountId(any(), any());
    }

    @Test
    @DisplayName("帳戶不存在應拋出 AccountNotFoundException")
    void should_throw_when_account_not_found() {
        given(loadAccountPort.findByAccountId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(QueryFixture.twdQuery("C001", "00123456789012")))
            .isInstanceOf(AccountNotFoundException.class);

        then(loadTransactionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("非帳戶持有人查詢應拋出 AccountNotOwnedByCustomerException")
    void should_throw_when_not_account_owner() {
        var mockAccount = AccountTestFixture.activeTwdAccount(CustomerId.of("C001"));
        given(loadAccountPort.findByAccountId(any())).willReturn(Optional.of(mockAccount));

        var query = QueryFixture.twdQuery("C999", "00123456789012");

        assertThatThrownBy(() -> handler.execute(query))
            .isInstanceOf(AccountNotOwnedByCustomerException.class);

        then(loadTransactionPort).shouldHaveNoInteractions();
    }
}
