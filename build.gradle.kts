plugins {
    java
    id("org.springframework.boot") version "4.0.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.bank"
version = "0.0.1-SNAPSHOT"

java {
    // Tutorial targets Java 23; this environment runs Java 25 (a superset).
    // All language features used (records, pattern matching, text blocks) are available in both.
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

repositories {
    mavenCentral()
}

dependencies {
    // ── Spring Boot (Driving Adapter — REST) ──────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT 驗證

    // ── 持久化：JPA + PostgreSQL + Flyway ───────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-flyway")  // Spring Boot 4：Flyway 自動設定模組
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // ── Test ──────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test") // JUnit 5 + Mockito + AssertJ
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")  // @WebMvcTest slice (Spring Boot 4 module)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.jayway.jsonpath:json-path")

    // ── 整合測試：真實 PostgreSQL（Testcontainers 2.x，模組已改為 testcontainers- 前綴）──
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // ── BDD：Cucumber 7 + Gherkin（端對端情境測試）──────────────────────
    testImplementation(platform("io.cucumber:cucumber-bom:7.34.4"))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("org.junit.platform:junit-platform-suite")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // 讓 Testcontainers 走 podman（rootless）：沿用外部 DOCKER_HOST，並停用 ryuk（rootless 相容性）。
    System.getenv("DOCKER_HOST")?.let { environment("DOCKER_HOST", it) }
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}
