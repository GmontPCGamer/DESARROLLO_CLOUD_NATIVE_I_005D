package com.cloudnative.payment.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulador académico de pagos. No procesa tarjetas reales: autoriza cualquier
 * intento válido y, cuando la orden se confirma, RabbitMQ pasa el pago a CAPTURED.
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

  private final PaymentLedger ledger;

  public PaymentController(PaymentLedger ledger) {
    this.ledger = ledger;
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "payment-service");
  }

  @PostMapping("/payments/intent")
  public PaymentResponse intent(@RequestBody PaymentRequest request, @AuthenticationPrincipal Jwt jwt) {
    BigDecimal amount = request == null ? null : request.amount();
    String method = request == null ? null : request.method();
    return ledger.authorize(jwt.getSubject(), amount, method);
  }

  @GetMapping("/payments/{paymentId}")
  public PaymentResponse get(@PathVariable String paymentId, @AuthenticationPrincipal Jwt jwt) {
    return ledger.find(jwt.getSubject(), paymentId);
  }

  public record PaymentRequest(BigDecimal amount, String method) {
  }

  public record PaymentResponse(String paymentId, String status, BigDecimal amount, String method, Instant createdAt) {
  }
}
