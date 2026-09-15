package com.cloudnative.backend.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP del BFF hacia cada microservicio. El BFF reenvía el Bearer
 * token del usuario en todas las operaciones privadas; los errores 4xx/5xx y
 * la indisponibilidad se traducen en {@code ApiExceptionHandler}.
 */
@Component
public class StoreGateway {

  private final RestClient catalog;
  private final RestClient cart;
  private final RestClient orders;
  private final RestClient notifications;
  private final RestClient inventory;
  private final RestClient payments;
  private final RestClient shipping;
  private final RestClient reviews;

  public StoreGateway(
      RestClient.Builder builder,
      @Value("${app.services.catalog}") String catalogUrl,
      @Value("${app.services.cart}") String cartUrl,
      @Value("${app.services.orders}") String ordersUrl,
      @Value("${app.services.notifications}") String notificationsUrl,
      @Value("${app.services.inventory}") String inventoryUrl,
      @Value("${app.services.payments}") String paymentsUrl,
      @Value("${app.services.shipping}") String shippingUrl,
      @Value("${app.services.reviews}") String reviewsUrl) {
    this.catalog = builder.clone().baseUrl(catalogUrl).build();
    this.cart = builder.clone().baseUrl(cartUrl).build();
    this.orders = builder.clone().baseUrl(ordersUrl).build();
    this.notifications = builder.clone().baseUrl(notificationsUrl).build();
    this.inventory = builder.clone().baseUrl(inventoryUrl).build();
    this.payments = builder.clone().baseUrl(paymentsUrl).build();
    this.shipping = builder.clone().baseUrl(shippingUrl).build();
    this.reviews = builder.clone().baseUrl(reviewsUrl).build();
  }

  // ---- Catálogo (público) ---------------------------------------------------

  public List<ProductDto> products(String category) {
    var spec = catalog.get();
    if (category == null || category.isBlank()) {
      return spec.uri("/api/products").retrieve().body(new ParameterizedTypeReference<>() {});
    }
    return spec.uri("/api/products?category={category}", category)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public ProductDto product(Long id) {
    return catalog.get().uri("/api/products/{id}", id).retrieve().body(ProductDto.class);
  }

  public ProductDto restockCatalog(String token, Long id, int quantity) {
    return catalog.post()
        .uri("/api/products/{id}/restock", id)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("quantity", quantity))
        .retrieve()
        .body(ProductDto.class);
  }

  // ---- Carrito --------------------------------------------------------------

  public CartDto cart(String token) {
    return cart.get().uri("/api/cart").headers(h -> h.setBearerAuth(token)).retrieve().body(CartDto.class);
  }

  public CartDto addItem(String token, Long productId, int quantity) {
    return cart.post()
        .uri("/api/cart/items")
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("productId", productId, "quantity", quantity))
        .retrieve()
        .body(CartDto.class);
  }

  public CartDto updateItem(String token, Long productId, int quantity) {
    return cart.put()
        .uri("/api/cart/items/{productId}", productId)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("quantity", quantity))
        .retrieve()
        .body(CartDto.class);
  }

  public CartDto removeItem(String token, Long productId) {
    return cart.delete()
        .uri("/api/cart/items/{productId}", productId)
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(CartDto.class);
  }

  public CartDto clearCart(String token) {
    return cart.delete().uri("/api/cart").headers(h -> h.setBearerAuth(token)).retrieve().body(CartDto.class);
  }

  // ---- Órdenes --------------------------------------------------------------

  public List<OrderDto> orders(String token) {
    return orders.get()
        .uri("/api/orders")
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public OrderDto order(String token, Long id) {
    return orders.get().uri("/api/orders/{id}", id).headers(h -> h.setBearerAuth(token)).retrieve().body(OrderDto.class);
  }

  public OrderDto checkout(String token, Map<String, Object> checkout) {
    return orders.post()
        .uri("/api/orders")
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(checkout)
        .retrieve()
        .body(OrderDto.class);
  }

  // ---- Notificaciones -------------------------------------------------------

  public List<NotificationDto> notifications(String token) {
    return notifications.get()
        .uri("/api/notifications")
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public Map<String, Long> unread(String token) {
    return notifications.get()
        .uri("/api/notifications/unread-count")
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public NotificationDto markRead(String token, Long id) {
    return notifications.patch()
        .uri("/api/notifications/{id}/read", id)
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(NotificationDto.class);
  }

  public Map<String, Long> markAllRead(String token) {
    return notifications.patch()
        .uri("/api/notifications/read-all")
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  // ---- Inventario -----------------------------------------------------------

  public StockDto stock(String token, Long productId) {
    return inventory.get()
        .uri("/api/inventory/{id}", productId)
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(StockDto.class);
  }

  public List<StockDto> inventory(String token) {
    return inventory.get()
        .uri("/api/inventory")
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public StockDto releaseInventory(String token, Long productId, int quantity) {
    return inventory.post()
        .uri("/api/inventory/{id}/release", productId)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("quantity", quantity))
        .retrieve()
        .body(StockDto.class);
  }

  // ---- Pagos ----------------------------------------------------------------

  public PaymentDto payment(String token, BigDecimal amount, String method) {
    Map<String, Object> body = method == null
        ? Map.of("amount", amount)
        : Map.of("amount", amount, "method", method);
    return payments.post()
        .uri("/api/payments/intent")
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(PaymentDto.class);
  }

  // ---- Despacho -------------------------------------------------------------

  public ShippingDto shipping(String token, String region, String commune, BigDecimal subtotal) {
    Map<String, Object> body = new java.util.HashMap<>();
    body.put("region", region);
    body.put("commune", commune);
    body.put("subtotal", subtotal);
    return shipping.post()
        .uri("/api/shipping/quote")
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(ShippingDto.class);
  }

  // ---- Reseñas --------------------------------------------------------------

  public List<ReviewDto> reviews(Long productId) {
    return reviews.get()
        .uri("/api/reviews/{id}", productId)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public ReviewDto addReview(String token, Long productId, int rating, String comment) {
    Map<String, Object> body = new java.util.HashMap<>();
    body.put("rating", rating);
    body.put("comment", comment);
    return reviews.post()
        .uri("/api/reviews/{id}", productId)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(ReviewDto.class);
  }

  // ---- DTOs -----------------------------------------------------------------

  public record ProductDto(
      Long id,
      String name,
      String description,
      String category,
      String brand,
      BigDecimal price,
      String imageUrl,
      int stock) {
  }

  public record CartItemDto(Long productId, int quantity) {
  }

  public record CartDto(String userId, List<CartItemDto> items, int totalItems) {
  }

  public record OrderLineDto(Long productId, String productName, BigDecimal unitPrice, int quantity) {
  }

  public record OrderDto(
      Long id,
      String status,
      BigDecimal subtotal,
      BigDecimal shippingCost,
      BigDecimal total,
      String shippingService,
      String shippingRegion,
      String shippingCommune,
      String paymentId,
      String paymentMethod,
      Instant createdAt,
      List<OrderLineDto> lines) {
  }

  public record NotificationDto(Long id, String title, String message, String type, boolean read, Instant createdAt) {
  }

  public record StockDto(Long productId, int available, boolean inStock) {
  }

  public record PaymentDto(String paymentId, String status, BigDecimal amount, String method, Instant createdAt) {
  }

  public record ShippingDto(
      String service,
      BigDecimal price,
      String promise,
      Instant estimatedAt,
      String region,
      String commune,
      boolean free) {
  }

  public record ReviewDto(Long id, Long productId, String author, int rating, String comment, Instant createdAt) {
  }
}
