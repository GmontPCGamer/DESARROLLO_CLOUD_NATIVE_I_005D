package com.cloudnative.analytics.config;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
public class JwtDecoderConfig {
  @Bean
  @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "secret-key")
  JwtDecoder secretKeyJwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.secret-key}") String secretKey) {
    return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(Base64.getDecoder().decode(secretKey), "HmacSHA256"))
        .macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "issuer-uri")
  JwtDecoder issuerUriJwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${app.security.audience}") String audience) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
    Set<String> accepted = new LinkedHashSet<>();
    Arrays.stream(audience.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(value -> {
      accepted.add(value);
      if (!value.startsWith("api://")) accepted.add("api://" + value);
    });
    OAuth2TokenValidator<Jwt> aud = new JwtClaimValidator<List<String>>("aud",
        values -> values != null && values.stream().anyMatch(accepted::contains));
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), aud));
    return decoder;
  }
}
