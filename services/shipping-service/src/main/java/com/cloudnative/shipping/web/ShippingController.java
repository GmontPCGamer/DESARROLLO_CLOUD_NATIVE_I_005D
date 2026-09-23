package com.cloudnative.shipping.web;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

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

import com.cloudnative.shipping.messaging.ShipmentBook;

/**
 * Cotizador de despacho simulado. Reglas:
 * - Región Metropolitana: EXPRESS, $4.990, 1 a 2 días hábiles.
 * - Resto del país: STANDARD, $7.990, 3 a 5 días hábiles.
 * - Compras desde $500.000 tienen despacho gratis.
 */
@RestController
@RequestMapping("/api")
public class ShippingController {

  static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("500000");
  static final BigDecimal EXPRESS_PRICE = new BigDecimal("4990");
  static final BigDecimal STANDARD_PRICE = new BigDecimal("7990");

  private final ShipmentBook shipments;

  public ShippingController(ShipmentBook shipments) {
    this.shipments = shipments;
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "shipping-service");
  }

  @PostMapping("/shipping/quote")
  public ShippingQuote quote(@RequestBody AddressRequest request, @AuthenticationPrincipal Jwt jwt) {
    if (request == null || request.commune() == null || request.commune().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La comuna de despacho es obligatoria");
    }
    String region = request.region() == null || request.region().isBlank() ? "Metropolitana" : request.region().trim();
    boolean metropolitana = region.toLowerCase().contains("metropolitana") || region.equalsIgnoreCase("RM");

    String service = metropolitana ? "EXPRESS" : "STANDARD";
    BigDecimal price = metropolitana ? EXPRESS_PRICE : STANDARD_PRICE;
    int days = metropolitana ? 2 : 5;
    String promise = metropolitana ? "Entrega en 1 a 2 días hábiles" : "Entrega en 3 a 5 días hábiles";

    boolean free = request.subtotal() != null && request.subtotal().compareTo(FREE_SHIPPING_FROM) >= 0;
    if (free) {
      price = BigDecimal.ZERO;
      promise += " · Despacho gratis por compras sobre $500.000";
    }

    return new ShippingQuote(
        service,
        price,
        promise,
        Instant.now().plus(Duration.ofDays(days)),
        region,
        request.commune().trim(),
        free);
  }

  @GetMapping("/shipping/{trackingId}")
  public Tracking tracking(@PathVariable String trackingId, @AuthenticationPrincipal Jwt jwt) {
    ShipmentBook.Shipment shipment = shipments.find(trackingId);
    if (shipment != null && jwt.getSubject().equals(shipment.userId())) {
      return new Tracking(shipment.trackingId(), shipment.status(), shipment.message(), shipment.updatedAt());
    }
    return new Tracking(trackingId, "PREPARANDO", "Tu pedido está siendo preparado", Instant.now());
  }

  public record AddressRequest(String region, String commune, BigDecimal subtotal) {
  }

  public record ShippingQuote(
      String service,
      BigDecimal price,
      String promise,
      Instant estimatedAt,
      String region,
      String commune,
      boolean free) {
  }

  public record Tracking(String trackingId, String status, String message, Instant updatedAt) {
  }
}
