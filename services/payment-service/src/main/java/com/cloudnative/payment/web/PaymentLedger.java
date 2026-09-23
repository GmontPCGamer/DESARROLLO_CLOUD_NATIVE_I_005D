package com.cloudnative.payment.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.cloudnative.payment.web.PaymentController.PaymentResponse;

@Component
public class PaymentLedger {

  private static final Set<String> METHODS = Set.of("SIMULATED_CARD", "DEBIT", "CREDIT", "TRANSFER");

  private final Map<String, StoredPayment> payments = new ConcurrentHashMap<>();

  public PaymentResponse authorize(String userId, BigDecimal amount, String method) {
    if (amount == null || amount.signum() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto a pagar debe ser mayor que cero");
    }
    String normalized = method == null || method.isBlank() ? "SIMULATED_CARD" : method.trim().toUpperCase();
    if (!METHODS.contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago no soportado: " + normalized);
    }
    PaymentResponse response = new PaymentResponse(
        UUID.randomUUID().toString(), "AUTHORIZED", amount, normalized, Instant.now());
    payments.put(response.paymentId(), new StoredPayment(userId, response));
    return response;
  }

  public PaymentResponse find(String userId, String paymentId) {
    StoredPayment stored = payments.get(paymentId);
    if (stored == null || !stored.userId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
    }
    return stored.payment();
  }

  /** Pasa el pago de AUTHORIZED a CAPTURED cuando la orden quedó confirmada. */
  public boolean capture(String paymentId, String userId) {
    StoredPayment stored = payments.get(paymentId);
    if (stored == null || userId == null || !stored.userId().equals(userId)) {
      return false;
    }
    PaymentResponse current = stored.payment();
    if ("CAPTURED".equals(current.status())) {
      return true;
    }
    payments.put(paymentId, new StoredPayment(userId, new PaymentResponse(
        current.paymentId(), "CAPTURED", current.amount(), current.method(), current.createdAt())));
    return true;
  }

  private record StoredPayment(String userId, PaymentResponse payment) {
  }
}
