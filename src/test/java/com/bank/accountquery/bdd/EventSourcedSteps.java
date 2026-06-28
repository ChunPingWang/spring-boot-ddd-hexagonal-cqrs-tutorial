package com.bank.accountquery.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.accountquery.domain.model.privilege.PrivilegeType;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;

/**
 * 事件溯源情境的 When/Then 步驟（打 /api/v1/es/privileges 端點）。
 */
public class EventSourcedSteps {

    private final ApiClient api;
    private final ScenarioContext context;

    public EventSourcedSteps(ApiClient api, ScenarioContext context) {
        this.api = api;
        this.context = context;
    }

    @When("客戶以事件溯源核發優惠 {string} 類型 {string} 總次數 {int} 有效期 {string} 至 {string}")
    public void grant(String privilegeId, String typeDesc, int total, String validFrom, String validTo) {
        String body = "{\"type\":\"%s\",\"totalQuota\":%d,\"validFrom\":\"%s\",\"validTo\":\"%s\"}"
            .formatted(privilegeType(typeDesc).name(), total, validFrom, validTo);
        post("/api/v1/es/privileges/%s/grant".formatted(privilegeId), body);
    }

    @When("客戶以事件溯源使用優惠 {string} 一次 節省 {string} 轉帳至 {string}")
    public void use(String privilegeId, String savedAmount, String targetAccountNo) {
        String body = "{\"targetAccountNo\":\"%s\",\"savedAmount\":%s}".formatted(targetAccountNo, savedAmount);
        post("/api/v1/es/privileges/%s/use".formatted(privilegeId), body);
    }

    @When("客戶重播優惠 {string} 的狀態")
    public void replayState(String privilegeId) {
        get("/api/v1/es/privileges/%s".formatted(privilegeId));
    }

    @When("客戶檢視優惠 {string} 的事件流")
    public void viewEvents(String privilegeId) {
        get("/api/v1/es/privileges/%s/events".formatted(privilegeId));
    }

    @Then("重播後剩餘次數為 {int}")
    public void replayedRemainingIs(int expected) {
        assertThat(((Number) JsonPath.read(context.body, "$.data.remainingQuota")).intValue()).isEqualTo(expected);
    }

    @Then("事件流應有 {int} 筆事件")
    public void eventCountIs(int expected) {
        assertThat(((List<?>) JsonPath.read(context.body, "$.data")).size()).isEqualTo(expected);
    }

    @Then("事件流第 {int} 筆型別為 {string}")
    public void eventTypeAtIs(int index, String expectedType) {
        assertThat((String) JsonPath.read(context.body, "$.data[%d].eventType".formatted(index - 1)))
            .isEqualTo(expectedType);
    }

    private void post(String path, String body) {
        ApiClient.Response resp = api.post(path, body, context.customerId);
        context.statusCode = resp.status();
        context.body = resp.body();
    }

    private void get(String path) {
        ApiClient.Response resp = api.get(path, context.customerId);
        context.statusCode = resp.status();
        context.body = resp.body();
    }

    private static PrivilegeType privilegeType(String description) {
        for (PrivilegeType t : PrivilegeType.values()) {
            if (t.description().equals(description)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知的優惠類型：" + description);
    }
}
