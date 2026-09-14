package com.cloudnative.payment.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {
  @GetMapping("/public/health") public Map<String, String> health() { return Map.of("status", "UP", "service", "payment-service"); }
  @PostMapping("/payments/intent") public PaymentResponse intent(@RequestBody PaymentRequest request, @AuthenticationPrincipal Jwt jwt) {
    return new PaymentResponse(UUID.randomUUID().toString(), "AUTHORIZED", request.amount(), "SIMULATED_CARD", Instant.now());
  }
  public record PaymentRequest(BigDecimal amount, String method) {}
  public record PaymentResponse(String paymentId, String status, BigDecimal amount, String method, Instant createdAt) {}
}
