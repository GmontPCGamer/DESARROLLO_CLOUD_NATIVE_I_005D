package com.cloudnative.backend.config;

import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
    return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
  }
}