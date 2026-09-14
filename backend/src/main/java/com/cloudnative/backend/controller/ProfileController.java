package com.cloudnative.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProfileController {

  @GetMapping("/me")
  public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
    return Map.of(
        "sub", jwt.getSubject(),
        "name", jwt.getClaimAsString("name"),
        "preferred_username", jwt.getClaimAsString("preferred_username"),
        "email", jwt.getClaimAsString("preferred_username"),
        "tenant", jwt.getClaimAsString("tid"),
        "issuer", jwt.getIssuer(),
        "audience", jwt.getAudience(),
        "scope", jwt.getClaimAsString("scp"),
        "roles", List.copyOf(jwt.getClaimAsStringList("roles") != null
            ? jwt.getClaimAsStringList("roles")
            : List.of()));
  }
}