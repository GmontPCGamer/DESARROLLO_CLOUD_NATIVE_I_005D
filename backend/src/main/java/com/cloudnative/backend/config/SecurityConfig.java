package com.cloudnative.backend.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad del BFF.
 *
 * - Todas las rutas fuera de /api/public/** exigen un JWT válido (firma, issuer, audiencia, vigencia).
 * - Con el perfil azuread, además se exige el scope delegado {@code access_as_user}.
 * - /api/admin/** exige ROLE_ADMIN (claim {@code roles} de Entra o lista {@code app.security.admin-users}).
 * - 401 y 403 responden con un cuerpo JSON (RFC 7807) además del header WWW-Authenticate.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final CorsProperties corsProperties;
  private final boolean requireApiScope;
  private final Set<String> adminUsers;

  public SecurityConfig(
      CorsProperties corsProperties,
      @Value("${app.security.require-scope:false}") boolean requireApiScope,
      @Value("${app.security.admin-users:}") String adminUsers) {
    this.corsProperties = corsProperties;
    this.requireApiScope = requireApiScope;
    this.adminUsers = Arrays.stream(adminUsers.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
          auth.requestMatchers("/api/public/**").permitAll();
          auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
          if (requireApiScope) {
            auth.requestMatchers("/api/**").hasAuthority("SCOPE_access_as_user");
          }
          auth.anyRequest().authenticated();
        })
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            .authenticationEntryPoint(this::unauthorized)
            .accessDeniedHandler(this::forbidden));

    return http.build();
  }

  /**
   * Convierte claims en authorities:
   * - {@code scp}/{@code scope} → SCOPE_x (comportamiento estándar).
   * - {@code roles} (app roles de Entra) → ROLE_x.
   * - usuarios listados en app.security.admin-users → ROLE_ADMIN.
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
      List<String> roles = jwt.getClaimAsStringList("roles");
      if (roles != null) {
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))));
      }
      if (isConfiguredAdmin(jwt)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      }
      return authorities;
    });
    return converter;
  }

  private boolean isConfiguredAdmin(Jwt jwt) {
    if (adminUsers.isEmpty()) {
      return false;
    }
    for (String claim : List.of("preferred_username", "email", "oid", "sub")) {
      String value = jwt.getClaimAsString(claim);
      if (value != null && adminUsers.contains(value.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(corsProperties.allowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("WWW-Authenticate"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  // ---- Respuestas 401 / 403 con cuerpo JSON --------------------------------

  private void unauthorized(
      jakarta.servlet.http.HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.core.AuthenticationException exception) throws IOException {
    // Mantiene el header WWW-Authenticate estándar (error, error_description).
    new BearerTokenAuthenticationEntryPoint().commence(request, response, exception);
    String detail = exception instanceof InvalidBearerTokenException
        ? "Token inválido o expirado: " + exception.getMessage()
        : "Debes enviar un token Bearer emitido por Microsoft Entra ID";
    writeProblem(response, HttpStatus.UNAUTHORIZED, detail, request.getRequestURI());
  }

  private void forbidden(
      jakarta.servlet.http.HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException exception) throws IOException {
    new BearerTokenAccessDeniedHandler().handle(request, response, exception);
    writeProblem(response, HttpStatus.FORBIDDEN,
        "El token es válido pero no tiene el scope o rol requerido para este recurso", request.getRequestURI());
  }

  private static void writeProblem(HttpServletResponse response, HttpStatus status, String detail, String path)
      throws IOException {
    // Mismo formato RFC 7807 que produce Spring para ProblemDetail.
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", "about:blank");
    problem.put("title", status.getReasonPhrase());
    problem.put("status", status.value());
    problem.put("detail", detail);
    problem.put("instance", path);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(JSON.writeValueAsString(problem));
  }
}
