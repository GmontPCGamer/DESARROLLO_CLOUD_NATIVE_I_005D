package com.cloudnative.inventory.config;

import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;

@Configuration(proxyBeanMethods = false)
public class JwtDecoderConfig {
  @Bean @ConditionalOnProperty(prefix="spring.security.oauth2.resourceserver.jwt", name="secret-key") JwtDecoder secretKeyJwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.secret-key}") String secret) { return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build(); }
  @Bean @ConditionalOnProperty(prefix="spring.security.oauth2.resourceserver.jwt", name="issuer-uri") JwtDecoder issuerUriJwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer, @Value("${app.security.audience}") String audience) { NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build(); OAuth2TokenValidator<Jwt> validator = new JwtClaimValidator<List<String>>("aud", values -> values != null && values.contains(audience)); decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), validator)); return decoder; }
}
