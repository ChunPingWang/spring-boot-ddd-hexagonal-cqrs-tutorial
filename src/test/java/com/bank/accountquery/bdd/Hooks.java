package com.bank.accountquery.bdd;

import com.bank.accountquery.infrastructure.adapter.out.persistence.inmemory.InMemoryBankingDataStore;
import io.cucumber.java.Before;

/**
 * 每個情境開始前清空資料源與情境狀態，確保情境彼此獨立。
 */
public class Hooks {

    private final InMemoryBankingDataStore store;
    private final ScenarioContext context;

    public Hooks(InMemoryBankingDataStore store, ScenarioContext context) {
        this.store = store;
        this.context = context;
    }

    @Before
    public void resetState() {
        store.reset();
        context.reset();
    }
}
