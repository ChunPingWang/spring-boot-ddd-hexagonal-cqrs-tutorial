package com.bank.accountquery.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import java.math.BigDecimal;
import java.util.List;

/**
 * Then 步驟：對 HTTP 回應做斷言（狀態碼、回應代碼、清單筆數、欄位值）。
 * 金額與匯率以數值比較，避免小數位數差異造成誤判。
 */
public class AssertionSteps {

    private final ScenarioContext context;

    public AssertionSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("回應狀態碼為 {int}")
    public void statusCodeIs(int expected) {
        assertThat(context.statusCode).isEqualTo(expected);
    }

    @Then("回應代碼為 {string}")
    public void responseCodeIs(String expected) {
        assertThat((String) read("$.code")).isEqualTo(expected);
    }

    @Then("錯誤訊息包含 {string}")
    public void messageContains(String fragment) {
        assertThat((String) read("$.message")).contains(fragment);
    }

    @Then("應回傳 {int} 筆交易紀錄")
    public void transactionCountIs(int expected) {
        assertThat(arraySize("$.data.transactions")).isEqualTo(expected);
    }

    @Then("第一筆交易類型為 {string}")
    public void firstTransactionTypeIs(String expected) {
        assertThat((String) read("$.data.transactions[0].transactionType")).isEqualTo(expected);
    }

    @Then("第一筆交易金額為 {string}")
    public void firstTransactionAmountIs(String expected) {
        assertNumericEquals("$.data.transactions[0].amount", expected);
    }

    @Then("第一筆外幣交易原幣金額為 {string}")
    public void firstFxAmountIs(String expected) {
        assertNumericEquals("$.data.transactions[0].fxAmount", expected);
    }

    @Then("交易紀錄包含台幣等值 {string}")
    public void twdEquivalentIs(String expected) {
        assertNumericEquals("$.data.transactions[0].twdEquivalent", expected);
    }

    @Then("交易紀錄顯示匯率 {string}")
    public void exchangeRateIs(String expected) {
        assertNumericEquals("$.data.transactions[0].exchangeRate", expected);
    }

    @Then("應回傳 {int} 筆優惠")
    public void privilegeCountIs(int expected) {
        assertThat(arraySize("$.data.privileges")).isEqualTo(expected);
    }

    @Then("優惠 {string} 剩餘次數為 {int}")
    public void remainingQuotaIs(String privilegeId, int expected) {
        assertThat(((Number) privilegeField(privilegeId, "remainingQuota")).intValue()).isEqualTo(expected);
    }

    @Then("優惠 {string} 為有效")
    public void privilegeIsValid(String privilegeId) {
        assertThat(privilegeField(privilegeId, "isValid")).isEqualTo(Boolean.TRUE);
    }

    @Then("優惠 {string} 為無效")
    public void privilegeIsInvalid(String privilegeId) {
        assertThat(privilegeField(privilegeId, "isValid")).isEqualTo(Boolean.FALSE);
    }

    @Then("優惠 {string} 為已過期")
    public void privilegeIsExpired(String privilegeId) {
        assertThat(privilegeField(privilegeId, "expired")).isEqualTo(Boolean.TRUE);
    }

    @Then("應回傳 {int} 筆使用紀錄")
    public void usageRecordCountIs(int expected) {
        assertThat(arraySize("$.data.records")).isEqualTo(expected);
    }

    @Then("優惠使用結果剩餘次數為 {int}")
    public void usedResultRemainingIs(int expected) {
        assertThat(((Number) read("$.data.remainingQuota")).intValue()).isEqualTo(expected);
    }

    @Then("優惠使用結果已用次數為 {int}")
    public void usedResultUsedIs(int expected) {
        assertThat(((Number) read("$.data.usedQuota")).intValue()).isEqualTo(expected);
    }

    @Then("總節省金額為 {string}")
    public void totalSavedIs(String expected) {
        assertNumericEquals("$.data.totalSaved", expected);
    }

    // ── helpers ────────────────────────────────────────────────────────
    private <T> T read(String path) {
        return JsonPath.read(context.body, path);
    }

    private int arraySize(String path) {
        return ((List<?>) read(path)).size();
    }

    private void assertNumericEquals(String path, String expected) {
        Object actual = read(path);
        assertThat(new BigDecimal(String.valueOf(actual)))
            .as("JSON path %s", path)
            .isEqualByComparingTo(new BigDecimal(expected));
    }

    private Object privilegeField(String privilegeId, String field) {
        List<Object> values = JsonPath.read(context.body,
            "$.data.privileges[?(@.privilegeId=='%s')].%s".formatted(privilegeId, field));
        assertThat(values).as("優惠 %s 應存在於回應中", privilegeId).isNotEmpty();
        return values.get(0);
    }
}
