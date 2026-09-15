package com.cloudnative.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone al frontend los claims relevantes del token ya validado por el BFF.
 * Nunca devuelve el JWT completo.
 */
@RestController
@RequestMapping("/api")
public class ProfileController {

  @GetMapping("/me")
  public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    // LinkedHashMap admite valores null (Map.of no) y conserva el orden para la demo.
    Map<String, Object> me = new LinkedHashMap<>();
    me.put("sub", jwt.getSubject());
    me.put("oid", jwt.getClaimAsString("oid"));
    me.put("name", jwt.getClaimAsString("name"));
    me.put("preferred_username", jwt.getClaimAsString("preferred_username"));
    me.put("email", firstNonNull(jwt.getClaimAsString("email"), jwt.getClaimAsString("preferred_username")));
    me.put("tenant", jwt.getClaimAsString("tid"));
    me.put("issuer", jwt.getClaimAsString("iss"));
    me.put("audience", jwt.getAudience());
    me.put("scope", jwt.getClaimAsString("scp"));
    me.put("scopes", scopes(jwt));
    me.put("roles", roles(jwt));
    me.put("authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    me.put("isAdmin", authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    me.put("issuedAt", jwt.getIssuedAt());
    me.put("expiresAt", jwt.getExpiresAt());
    me.put("tokenVersion", jwt.getClaimAsString("ver"));
    return me;
  }

  private static List<String> scopes(Jwt jwt) {
    String scp = jwt.getClaimAsString("scp");
    if (scp == null || scp.isBlank()) {
      List<String> scope = jwt.getClaimAsStringList("scope");
      return scope == null ? List.of() : scope;
    }
    return List.of(scp.trim().split("\\s+"));
  }

  private static List<String> roles(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    return roles == null ? List.of() : List.copyOf(roles);
  }

  private static String firstNonNull(String first, String second) {
    return first != null ? first : second;
  }
}
