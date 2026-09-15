package com.cloudnative.payment.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulador académico de pagos. No procesa tarjetas reales: autoriza cualquier
 * intento válido y guarda un registro en memoria asociado al usuario (sub).
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

  private static final Set<String> METHODS = Set.of("SIMULATED_CARD", "DEBIT", "CREDIT", "TRANSFER");

  private final Map<String, StoredPayment> payments = new ConcurrentHashMap<>();

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "payment-service");
  }

  @PostMapping("/payments/intent")
  public PaymentResponse intent(@RequestBody PaymentRequest request, @AuthenticationPrincipal Jwt jwt) {
    if (request == null || request.amount() == null || request.amount().signum() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto a pagar debe ser mayor que cero");
    }
    String method = request.method() == null || request.method().isBlank()
        ? "SIMULATED_CARD"
        : request.method().trim().toUpperCase();
    if (!METHODS.contains(method)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago no soportado: " + method);
    }
    StoredPayment stored = new StoredPayment(
        jwt.getSubject(),
        new PaymentResponse(UUID.randomUUID().toString(), "AUTHORIZED", request.amount(), method, Instant.now()));
    payments.put(stored.payment().paymentId(), stored);
    return stored.payment();
  }

  @GetMapping("/payments/{paymentId}")
  public PaymentResponse get(@PathVariable String paymentId, @AuthenticationPrincipal Jwt jwt) {
    StoredPayment stored = payments.get(paymentId);
    if (stored == null || !stored.userId().equals(jwt.getSubject())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
    }
    return stored.payment();
  }

  private record StoredPayment(String userId, PaymentResponse payment) {
  }

  public record PaymentRequest(BigDecimal amount, String method) {
  }

  public record PaymentResponse(String paymentId, String status, BigDecimal amount, String method, Instant createdAt) {
  }
}
