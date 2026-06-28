package com.bank.accountquery.bdd;

import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.AccountJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.PrivilegeJpaRepository;
import com.bank.accountquery.infrastructure.adapter.out.persistence.jpa.repository.TransactionJpaRepository;
import io.cucumber.java.Before;

/**
 * 每個情境開始前清空資料庫與情境狀態，確保情境彼此獨立。
 * 刪除順序遵守外鍵：交易 → 優惠（cascade 使用紀錄）→ 帳戶。
 */
public class Hooks {

    private final AccountJpaRepository accountRepository;
    private final TransactionJpaRepository transactionRepository;
    private final PrivilegeJpaRepository privilegeRepository;
    private final ScenarioContext context;

    public Hooks(AccountJpaRepository accountRepository,
                 TransactionJpaRepository transactionRepository,
                 PrivilegeJpaRepository privilegeRepository,
                 ScenarioContext context) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.privilegeRepository = privilegeRepository;
        this.context = context;
    }

    @Before
    public void resetState() {
        transactionRepository.deleteAll();
        privilegeRepository.deleteAll();   // cascade 刪除 usage records
        accountRepository.deleteAll();
        context.reset();
    }
}
