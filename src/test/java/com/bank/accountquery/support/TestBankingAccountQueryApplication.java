package com.bank.accountquery.support;

import com.bank.accountquery.BankingAccountQueryApplication;
import org.springframework.boot.SpringApplication;

/**
 * 本機開發啟動器：以 Testcontainers 提供 PostgreSQL，免手動架資料庫。
 * 執行：./gradlew bootTestRun
 */
public final class TestBankingAccountQueryApplication {

    public static void main(String[] args) {
        SpringApplication.from(BankingAccountQueryApplication::main)
            .with(TestcontainersConfiguration.class)
            .run(args);
    }
}
