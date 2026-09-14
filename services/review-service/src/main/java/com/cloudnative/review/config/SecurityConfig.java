package com.cloudnative.review.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Value("${app.security.require-scope:false}") private boolean requireScope;
  @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { http.csrf(csrf -> csrf.disable()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(auth -> { auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll(); auth.requestMatchers("/api/public/**").permitAll(); auth.requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll(); if (requireScope) auth.requestMatchers("/api/**").hasAuthority("SCOPE_access_as_user"); auth.anyRequest().authenticated(); }).oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())); return http.build(); }
}
