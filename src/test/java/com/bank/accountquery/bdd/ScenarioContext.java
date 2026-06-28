package com.bank.accountquery.bdd;

import org.springframework.stereotype.Component;

/**
 * 跨 Step Definition 共用的情境狀態（單例 Bean，於每個情境開始前由 Hooks 重置）。
 */
@Component
public class ScenarioContext {

    /** 目前已認證客戶；null 代表未認證。 */
    public String customerId;

    /** 最近一次 HTTP 回應狀態碼。 */
    public int statusCode;

    /** 最近一次 HTTP 回應的 JSON 內容。 */
    public String body;

    public void reset() {
        this.customerId = null;
        this.statusCode = 0;
        this.body = null;
    }
}
