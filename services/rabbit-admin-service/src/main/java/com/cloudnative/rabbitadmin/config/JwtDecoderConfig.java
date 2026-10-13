package com.cloudnative.rabbitadmin.config;

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
  JwtDecoder secretKeyJwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.secret-key}") String secretKey) {
    byte[] keyBytes = Base64.getDecoder().decode(secretKey);
    SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "issuer-uri")
  JwtDecoder issuerUriJwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      @Value("${app.security.audience}") String audience) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    Set<String> accepted = acceptedAudiences(audience);
    OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
        "aud", audiences -> audiences != null && audiences.stream().anyMatch(accepted::contains));
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator));
    return decoder;
  }

  static Set<String> acceptedAudiences(String configured) {
    Set<String> accepted = new LinkedHashSet<>();
    Arrays.stream(configured.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .forEach(value -> {
          accepted.add(value);
          if (!value.startsWith("api://")) {
            accepted.add("api://" + value);
          }
        });
    return accepted;
  }
}
