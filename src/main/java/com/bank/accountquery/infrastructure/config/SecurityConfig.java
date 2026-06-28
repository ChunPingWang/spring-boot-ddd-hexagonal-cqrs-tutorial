package com.bank.accountquery.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 以 OAuth2 Resource Server 驗證 JWT（Bearer Token）。
 *
 * 設計重點：Security 屬於 Infrastructure。它只負責「驗證身分（authentication）」，
 * 至於「能不能查這個帳戶（authorization）」仍由 Domain 的 Aggregate（verifyOwnership）守護，
 * 因此這裡不做任何業務層級的授權判斷。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecretKey jwtSecretKey(
        @Value("${app.security.jwt.secret:demo-secret-key-please-change-in-production-0123456789}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        // 直接輸出統一回應格式的 JSON（401 由過濾鏈觸發，不經過 GlobalExceptionHandler）。
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            String body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"缺少或無效的身分認證\",\"timestamp\":\"%s\"}"
                .formatted(OffsetDateTime.now());
            response.getWriter().write(body);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint entryPoint) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(entryPoint))
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint));
        return http.build();
    }
}
