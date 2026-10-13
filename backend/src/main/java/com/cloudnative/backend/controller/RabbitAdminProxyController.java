package com.cloudnative.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * El BFF exige ROLE_ADMIN y reenvía el Bearer al microservicio administrador.
 */
@RestController
@RequestMapping("/api/admin/rabbit")
public class RabbitAdminProxyController {

  private final RestClient http;

  public RabbitAdminProxyController(@Value("${app.services.rabbit-admin}") String baseUrl) {
    this.http = RestClient.builder().baseUrl(baseUrl).build();
  }

  @PostMapping("/queues")
  public ResponseEntity<byte[]> createQueue(@RequestBody byte[] body, @AuthenticationPrincipal Jwt jwt) {
    return forward(HttpMethod.POST, "/queues", body, jwt);
  }

  @GetMapping("/queues/{name}")
  public ResponseEntity<byte[]> getQueue(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
    return forward(HttpMethod.GET, "/queues/" + name, null, jwt);
  }

  @DeleteMapping("/queues/{name}")
  public ResponseEntity<byte[]> deleteQueue(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
    return forward(HttpMethod.DELETE, "/queues/" + name, null, jwt);
  }

  @PostMapping("/exchanges")
  public ResponseEntity<byte[]> createExchange(@RequestBody byte[] body, @AuthenticationPrincipal Jwt jwt) {
    return forward(HttpMethod.POST, "/exchanges", body, jwt);
  }

  @DeleteMapping("/exchanges/{name}")
  public ResponseEntity<byte[]> deleteExchange(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
    return forward(HttpMethod.DELETE, "/exchanges/" + name, null, jwt);
  }

  @PostMapping("/bindings")
  public ResponseEntity<byte[]> createBinding(@RequestBody byte[] body, @AuthenticationPrincipal Jwt jwt) {
    return forward(HttpMethod.POST, "/bindings", body, jwt);
  }

  @DeleteMapping("/bindings")
  public ResponseEntity<byte[]> deleteBinding(@RequestBody byte[] body, @AuthenticationPrincipal Jwt jwt) {
    return forward(HttpMethod.DELETE, "/bindings", body, jwt);
  }

  private ResponseEntity<byte[]> forward(HttpMethod method, String path, byte[] body, Jwt jwt) {
    var spec = http.method(method)
        .uri(path)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
    if (body != null) {
      return spec.contentType(MediaType.APPLICATION_JSON).body(body).exchange(this::passthrough);
    }
    return spec.exchange(this::passthrough);
  }

  private ResponseEntity<byte[]> passthrough(org.springframework.http.HttpRequest request, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws java.io.IOException {
    return ResponseEntity.status(response.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(response.getBody().readAllBytes());
  }
}
