package com.bank.accountquery.infrastructure.adapter.in.rest.security;

import com.bank.accountquery.domain.model.shared.CustomerId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public class CurrentCustomerArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String CUSTOMER_HEADER = "X-Customer-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentCustomer.class)
            && parameter.getParameterType().equals(CustomerId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String customerId = webRequest.getHeader(CUSTOMER_HEADER);
        if (customerId == null || customerId.isBlank()) {
            // 模擬未提供有效 JWT → 401
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少身分認證資訊");
        }
        return CustomerId.of(customerId);
    }
}
