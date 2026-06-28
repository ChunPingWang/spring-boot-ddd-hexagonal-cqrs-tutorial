package com.bank.accountquery.bdd;

import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * 測試專用：以與應用程式相同的對稱密鑰簽出 HS256 JWT，subject 即客戶代號。
 * 讓 BDD 情境能用「真實」Bearer Token 打 API。
 */
@Component
public class JwtTokenFactory {

    private final JwtEncoder encoder;

    public JwtTokenFactory(SecretKey secretKey) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    public String tokenFor(String customerId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(customerId)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
