package com.cloudnative.backend.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudnative.backend.client.StoreGateway;
import com.cloudnative.backend.client.StoreGateway.CartDto;
import com.cloudnative.backend.client.StoreGateway.CartItemDto;
import com.cloudnative.backend.client.StoreGateway.NotificationDto;
import com.cloudnative.backend.client.StoreGateway.OrderDto;
import com.cloudnative.backend.client.StoreGateway.ProductDto;
import com.cloudnative.backend.client.StoreGateway.PaymentDto;
import com.cloudnative.backend.client.StoreGateway.ReviewDto;
import com.cloudnative.backend.client.StoreGateway.ShippingDto;
import com.cloudnative.backend.client.StoreGateway.StockDto;

@RestController
@RequestMapping("/api")
public class StoreController {

  private final StoreGateway store;

  public StoreController(StoreGateway store) {
    this.store = store;
  }

  @GetMapping("/public/products")
  public List<ProductDto> products(@RequestParam(required = false) String category) {
    List<ProductDto> products = store.products(category);
    return products == null ? List.of() : products;
  }

  @GetMapping("/public/products/{id}")
  public ProductDto product(@PathVariable Long id) {
    return store.product(id);
  }

  @GetMapping("/cart")
  public EnrichedCart cart(@AuthenticationPrincipal Jwt jwt) {
    return enrich(store.cart(jwt.getTokenValue()));
  }

  @PostMapping("/cart/items")
  public EnrichedCart addItem(@AuthenticationPrincipal Jwt jwt, @RequestBody AddItemRequest request) {
    return enrich(store.addItem(jwt.getTokenValue(), request.productId(), request.quantity()));
  }

  @PutMapping("/cart/items/{productId}")
  public EnrichedCart updateItem(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long productId,
      @RequestBody QtyRequest request) {
    return enrich(store.updateItem(jwt.getTokenValue(), productId, request.quantity()));
  }

  @DeleteMapping("/cart/items/{productId}")
  public EnrichedCart removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
    return enrich(store.removeItem(jwt.getTokenValue(), productId));
  }

  @GetMapping("/orders")
  public List<OrderDto> orders(@AuthenticationPrincipal Jwt jwt) {
    List<OrderDto> result = store.orders(jwt.getTokenValue());
    return result == null ? List.of() : result;
  }

  @PostMapping("/orders")
  public OrderDto checkout(@AuthenticationPrincipal Jwt jwt) {
    return store.checkout(jwt.getTokenValue());
  }

  @GetMapping("/notifications")
  public List<NotificationDto> notifications(@AuthenticationPrincipal Jwt jwt) {
    List<NotificationDto> result = store.notifications(jwt.getTokenValue());
    return result == null ? List.of() : result;
  }

  @GetMapping("/notifications/unread-count")
  public java.util.Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
    return store.unread(jwt.getTokenValue());
  }

  @PatchMapping("/notifications/{id}/read")
  public NotificationDto markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return store.markRead(jwt.getTokenValue(), id);
  }

  @GetMapping("/products/{productId}/stock")
  public StockDto stock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
    return store.stock(jwt.getTokenValue(), productId);
  }

  @PostMapping("/payments/intent")
  public PaymentDto payment(@AuthenticationPrincipal Jwt jwt, @RequestBody PaymentRequest request) {
    return store.payment(jwt.getTokenValue(), request.amount(), request.method());
  }

  @PostMapping("/shipping/quote")
  public ShippingDto shipping(@AuthenticationPrincipal Jwt jwt, @RequestBody ShippingRequest request) {
    return store.shipping(jwt.getTokenValue(), request.region(), request.commune());
  }

  @GetMapping("/public/products/{productId}/reviews")
  public List<ReviewDto> reviews(@PathVariable Long productId) {
    return store.reviews(productId);
  }

  @PostMapping("/products/{productId}/reviews")
  public ReviewDto addReview(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId, @RequestBody ReviewRequest request) {
    return store.addReview(jwt.getTokenValue(), productId, request.rating(), request.comment());
  }

  private EnrichedCart enrich(CartDto cart) {
    if (cart == null || cart.items() == null) {
      return new EnrichedCart(List.of(), BigDecimal.ZERO, 0);
    }
    List<EnrichedItem> items = cart.items().stream().map(this::enrichItem).toList();
    BigDecimal total = items.stream().map(EnrichedItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new EnrichedCart(items, total, cart.totalItems());
  }

  private EnrichedItem enrichItem(CartItemDto item) {
    ProductDto product = store.product(item.productId());
    BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(item.quantity()));
    return new EnrichedItem(product, item.quantity(), subtotal);
  }

  public record AddItemRequest(Long productId, int quantity) {
  }

  public record QtyRequest(int quantity) {
  }

  public record PaymentRequest(BigDecimal amount, String method) {}
  public record ShippingRequest(String region, String commune) {}
  public record ReviewRequest(int rating, String comment) {}

  public record EnrichedItem(ProductDto product, int quantity, BigDecimal subtotal) {
  }

  public record EnrichedCart(List<EnrichedItem> items, BigDecimal total, int totalItems) {
  }
}
