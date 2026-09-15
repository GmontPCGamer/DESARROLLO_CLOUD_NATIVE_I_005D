package com.cloudnative.inventory.web;

import java.util.List;
import java.util.Map;
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
 * Inventario reservable. Es la fuente de verdad de disponibilidad durante el
 * checkout: el servicio de órdenes reserva aquí antes de confirmar una compra
 * y libera si algo falla (compensación).
 *
 * La semilla está alineada con {@code ProductSeeder} del catálogo (ids 1..16).
 */
@RestController
@RequestMapping("/api")
public class InventoryController {

  private final Map<Long, Integer> stock = new ConcurrentHashMap<>();

  public InventoryController() {
    int[] seed = { 12, 15, 10, 8, 6, 5, 7, 4, 9, 8, 14, 11, 20, 25, 9, 18 };
    for (int i = 0; i < seed.length; i++) {
      stock.put((long) (i + 1), seed[i]);
    }
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "inventory-service");
  }

  @GetMapping("/inventory")
  public List<StockResponse> all() {
    return stock.keySet().stream().sorted().map(this::response).toList();
  }

  @GetMapping("/inventory/{productId}")
  public StockResponse get(@PathVariable Long productId) {
    return response(productId);
  }

  @PostMapping("/inventory/{productId}/reserve")
  public StockResponse reserve(
      @PathVariable Long productId,
      @RequestBody QuantityRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    validate(request);
    // compute() es atómico por clave: evita sobre-reservar bajo concurrencia.
    boolean[] reserved = { false };
    int remaining = stock.compute(productId, (id, available) -> {
      int current = available == null ? 0 : available;
      if (current < request.quantity()) {
        return current; // no alcanza: no se descuenta
      }
      reserved[0] = true;
      return current - request.quantity();
    });
    if (!reserved[0]) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Stock insuficiente en inventario: quedan " + remaining + " unidades del producto " + productId);
    }
    return response(productId);
  }

  /** Devuelve unidades (compensación de checkout o reposición). */
  @PostMapping("/inventory/{productId}/release")
  public StockResponse release(
      @PathVariable Long productId,
      @RequestBody QuantityRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    validate(request);
    stock.merge(productId, request.quantity(), Integer::sum);
    return response(productId);
  }

  private void validate(QuantityRequest request) {
    if (request == null || request.quantity() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser positiva");
    }
  }

  private StockResponse response(Long productId) {
    int available = stock.getOrDefault(productId, 0);
    return new StockResponse(productId, available, available > 0);
  }

  public record QuantityRequest(int quantity) {
  }

  public record StockResponse(Long productId, int available, boolean inStock) {
  }
}
