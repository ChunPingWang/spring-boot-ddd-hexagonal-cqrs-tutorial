package com.bank.accountquery.bdd;

import io.cucumber.java.en.When;

/**
 * When 步驟：以真實 HTTP 打 API，並把回應存入 ScenarioContext。
 */
public class QuerySteps {

    private final ApiClient api;
    private final ScenarioContext context;

    public QuerySteps(ApiClient api, ScenarioContext context) {
        this.api = api;
        this.context = context;
    }

    @When("客戶查詢帳戶 {string} 從 {string} 到 {string} 的台幣交易紀錄")
    public void queryTwd(String accountId, String start, String end) {
        get("/api/v1/accounts/%s/transactions/twd?startDate=%s&endDate=%s".formatted(accountId, start, end), true);
    }

    @When("客戶查詢帳戶 {string} 從 {string} 到 {string} 第 {int} 頁每頁 {int} 筆 的台幣交易紀錄")
    public void queryTwdPaged(String accountId, String start, String end, int page, int size) {
        get("/api/v1/accounts/%s/transactions/twd?startDate=%s&endDate=%s&page=%d&size=%d"
            .formatted(accountId, start, end, page, size), true);
    }

    @When("客戶在未提供身分認證下查詢帳戶 {string} 從 {string} 到 {string} 的台幣交易紀錄")
    public void queryTwdNoAuth(String accountId, String start, String end) {
        get("/api/v1/accounts/%s/transactions/twd?startDate=%s&endDate=%s".formatted(accountId, start, end), false);
    }

    @When("客戶查詢帳戶 {string} 幣別 {string} 從 {string} 到 {string} 的外幣交易紀錄")
    public void queryFx(String accountId, String currency, String start, String end) {
        get("/api/v1/accounts/%s/transactions/fx?currency=%s&startDate=%s&endDate=%s"
            .formatted(accountId, currency, start, end), true);
    }

    @When("客戶查詢轉帳優惠內容")
    public void queryPrivileges() {
        get("/api/v1/customers/me/privileges/transfer", true);
    }

    @When("客戶查詢優惠 {string} 從 {string} 到 {string} 的使用紀錄")
    public void queryUsage(String privilegeId, String start, String end) {
        get("/api/v1/customers/me/privileges/transfer/%s/usage?startDate=%s&endDate=%s"
            .formatted(privilegeId, start, end), true);
    }

    private void get(String path, boolean authenticate) {
        String customerId = authenticate ? context.customerId : null;
        ApiClient.Response resp = api.get(path, customerId);
        context.statusCode = resp.status();
        context.body = resp.body();
    }
}
