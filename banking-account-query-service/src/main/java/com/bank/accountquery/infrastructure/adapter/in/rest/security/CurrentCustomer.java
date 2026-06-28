package com.bank.accountquery.infrastructure.adapter.in.rest.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 將已認證客戶的 CustomerId 注入 Controller 方法參數。
 *
 * 註：本 Tutorial 切片以 X-Customer-Id Header 模擬 JWT SecurityContext 取出的客戶身分，
 * 以維持「Controller 不自行做認證」的職責邊界，同時可在 @WebMvcTest 中驗證。
 * 正式版應改為從 Spring Security 的 JWT Principal 取得。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentCustomer {
}
