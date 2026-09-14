package com.cloudnative.cart.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cloudnative.cart.domain.CartItem;
import com.cloudnative.cart.repo.CartItemRepository;

@RestController
@RequestMapping("/api")
public class CartController {

  private final CartItemRepository items;

  public CartController(CartItemRepository items) {
    this.items = items;
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "cart-service");
  }

  @GetMapping("/cart")
  public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
    return toResponse(jwt.getSubject());
  }

  @PostMapping("/cart/items")
  public CartResponse add(@AuthenticationPrincipal Jwt jwt, @RequestBody AddItemRequest request) {
    if (request.productId() == null || request.quantity() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto y cantidad son obligatorios");
    }
    String userId = jwt.getSubject();
    CartItem item = items.findByUserIdAndProductId(userId, request.productId()).orElseGet(() -> {
      CartItem created = new CartItem();
      created.setUserId(userId);
      created.setProductId(request.productId());
      created.setQuantity(0);
      return created;
    });
    item.setQuantity(item.getQuantity() + request.quantity());
    items.save(item);
    return toResponse(userId);
  }

  @PutMapping("/cart/items/{productId}")
  public CartResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long productId,
      @RequestBody UpdateQtyRequest request) {
    String userId = jwt.getSubject();
    CartItem item = items.findByUserIdAndProductId(userId, productId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem no está en el carrito"));
    if (request.quantity() < 1) {
      items.delete(item);
    } else {
      item.setQuantity(request.quantity());
      items.save(item);
    }
    return toResponse(userId);
  }

  @DeleteMapping("/cart/items/{productId}")
  @Transactional
  public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
    items.deleteByUserIdAndProductId(jwt.getSubject(), productId);
    return toResponse(jwt.getSubject());
  }

  @DeleteMapping("/cart")
  @Transactional
  public CartResponse clear(@AuthenticationPrincipal Jwt jwt) {
    items.deleteByUserId(jwt.getSubject());
    return toResponse(jwt.getSubject());
  }

  private CartResponse toResponse(String userId) {
    List<CartItemResponse> lines = items.findByUserId(userId).stream()
        .map(item -> new CartItemResponse(item.getProductId(), item.getQuantity()))
        .toList();
    int totalItems = lines.stream().mapToInt(CartItemResponse::quantity).sum();
    return new CartResponse(userId, lines, totalItems);
  }

  public record AddItemRequest(Long productId, int quantity) {
  }

  public record UpdateQtyRequest(int quantity) {
  }

  public record CartItemResponse(Long productId, int quantity) {
  }

  public record CartResponse(String userId, List<CartItemResponse> items, int totalItems) {
  }
}
