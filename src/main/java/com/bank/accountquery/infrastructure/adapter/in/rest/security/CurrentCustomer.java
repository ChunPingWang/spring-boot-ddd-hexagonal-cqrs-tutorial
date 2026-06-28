package com.bank.accountquery.infrastructure.adapter.in.rest.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 將已認證客戶的 CustomerId 注入 Controller 方法參數，
 * 來源為 Spring Security 驗證後的 JWT subject（見 {@link CurrentCustomerArgumentResolver}）。
 * 維持「Controller 不自行做認證」的職責邊界。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentCustomer {
}
