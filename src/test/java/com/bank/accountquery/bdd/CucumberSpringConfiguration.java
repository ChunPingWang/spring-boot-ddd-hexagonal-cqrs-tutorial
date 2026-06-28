package com.bank.accountquery.bdd;

import com.bank.accountquery.support.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 讓 Cucumber 共用 Spring Boot 應用情境：隨機埠啟動真實 HTTP 伺服器，
 * 並以 Testcontainers 提供真實 PostgreSQL，使情境走完整路徑：
 * Controller → Handler → Domain → JPA Adapter → PostgreSQL。
 * 關閉示範資料種子，改由各情境的 Background 自行佈置資料。
 */
@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.demo-seed.enabled=false")
@Import(TestcontainersConfiguration.class)
public class CucumberSpringConfiguration {
}
