package com.bank.accountquery.infrastructure.adapter.in.rest.security;

import com.bank.accountquery.domain.model.shared.CustomerId;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * 將已認證 JWT 的 subject（= 客戶代號）注入 Controller 方法參數。
 * 真正的身分驗證由 Spring Security 完成；此處僅從 SecurityContext 取出客戶身分，
 * Controller 因此完全不碰認證細節。
 */
public class CurrentCustomerArgumentResolver implements HandlerMethodArgumentResolver {

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String subject = jwt.getSubject();
            if (subject != null && !subject.isBlank()) {
                return CustomerId.of(subject);
            }
        }
        // 一般情況下 Spring Security 已於過濾鏈攔截未認證請求；此為防禦性後備。
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少身分認證資訊");
    }
}
