package com.cloudnative.shipping.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ShippingController {
  @GetMapping("/public/health") public Map<String, String> health() { return Map.of("status", "UP", "service", "shipping-service"); }
  @PostMapping("/shipping/quote") public ShippingQuote quote(@RequestBody AddressRequest request, @AuthenticationPrincipal Jwt jwt) {
    return new ShippingQuote("EXPRESS", new BigDecimal("4990"), "Entrega en 1 a 2 días hábiles", Instant.now().plusSeconds(172800));
  }
  @GetMapping("/shipping/{trackingId}") public Tracking tracking(@PathVariable String trackingId, @AuthenticationPrincipal Jwt jwt) {
    return new Tracking(trackingId, "PREPARANDO", "Tu pedido está siendo preparado", Instant.now());
  }
  public record AddressRequest(String region, String commune) {}
  public record ShippingQuote(String service, BigDecimal price, String promise, Instant estimatedAt) {}
  public record Tracking(String trackingId, String status, String message, Instant updatedAt) {}
}
