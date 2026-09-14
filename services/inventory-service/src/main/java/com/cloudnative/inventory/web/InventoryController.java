package com.cloudnative.inventory.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class InventoryController {
  private final Map<Long, Integer> stock = new ConcurrentHashMap<>();

  { stock.put(1L, 12); stock.put(2L, 15); stock.put(3L, 10); stock.put(4L, 8); stock.put(5L, 6); stock.put(6L, 5); stock.put(7L, 7); stock.put(8L, 4); stock.put(9L, 9); stock.put(10L, 8); }

  @GetMapping("/public/health") public Map<String, String> health() { return Map.of("status", "UP", "service", "inventory-service"); }
  @GetMapping("/inventory/{productId}") public StockResponse get(@PathVariable Long productId) { return response(productId); }
  @PostMapping("/inventory/{productId}/reserve") public StockResponse reserve(@PathVariable Long productId, @RequestBody QuantityRequest request, @AuthenticationPrincipal Jwt jwt) {
    if (request.quantity() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad inválida");
    int available = stock.getOrDefault(productId, 0);
    if (available < request.quantity()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente");
    stock.put(productId, available - request.quantity());
    return response(productId);
  }
  private StockResponse response(Long productId) { return new StockResponse(productId, stock.getOrDefault(productId, 0), stock.getOrDefault(productId, 0) > 0); }
  public record QuantityRequest(int quantity) {}
  public record StockResponse(Long productId, int available, boolean inStock) {}
}
