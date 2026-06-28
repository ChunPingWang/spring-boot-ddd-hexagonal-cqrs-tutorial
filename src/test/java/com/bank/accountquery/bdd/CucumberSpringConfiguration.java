package com.bank.accountquery.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 讓 Cucumber 共用 Spring Boot 應用情境，並以隨機埠啟動真實 HTTP 伺服器，
 * 使情境測試走完整路徑：Controller → Handler → Domain → Adapter。
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {
}
