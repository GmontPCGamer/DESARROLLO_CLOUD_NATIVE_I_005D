package com.cloudnative.backend.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.cloudnative.backend.client.StoreGateway;
import com.cloudnative.backend.client.StoreGateway.CartDto;
import com.cloudnative.backend.client.StoreGateway.CartItemDto;
import com.cloudnative.backend.client.StoreGateway.NotificationDto;
import com.cloudnative.backend.client.StoreGateway.OrderDto;
import com.cloudnative.backend.client.StoreGateway.PaymentDto;
import com.cloudnative.backend.client.StoreGateway.ProductDto;
import com.cloudnative.backend.client.StoreGateway.ReviewDto;
import com.cloudnative.backend.client.StoreGateway.ShippingDto;
import com.cloudnative.backend.client.StoreGateway.StockDto;

/**
 * Fachada única para Angular (BFF). Valida entradas, coordina microservicios y
 * adapta las respuestas a lo que necesita la interfaz.
 */
@RestController
@RequestMapping("/api")
public class StoreController {

  private static final int MAX_QTY_PER_LINE = 10;

  private final StoreGateway store;

  public StoreController(StoreGateway store) {
    this.store = store;
  }

  // ---- Catálogo público -----------------------------------------------------

  @GetMapping("/public/products")
  public List<ProductDto> products(@RequestParam(required = false) String category) {
    List<ProductDto> products = store.products(category);
    return products == null ? List.of() : products;
  }

  @GetMapping("/public/products/{id}")
  public ProductDto product(@PathVariable Long id) {
    return store.product(id);
  }

  @GetMapping("/public/products/{productId}/reviews")
  public List<ReviewDto> reviews(@PathVariable Long productId) {
    List<ReviewDto> result = store.reviews(productId);
    return result == null ? List.of() : result;
  }

  // ---- Carrito --------------------------------------------------------------

  @GetMapping("/cart")
  public EnrichedCart cart(@AuthenticationPrincipal Jwt jwt) {
    return enrich(store.cart(jwt.getTokenValue()));
  }

  @PostMapping("/cart/items")
  public EnrichedCart addItem(@AuthenticationPrincipal Jwt jwt, @RequestBody AddItemRequest request) {
    if (request == null || request.productId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar el producto");
    }
    int quantity = request.quantity() <= 0 ? 1 : request.quantity();
    ProductDto product = store.product(request.productId()); // 404 si no existe
    int alreadyInCart = quantityInCart(jwt.getTokenValue(), request.productId());
    assertStock(product, alreadyInCart + quantity);
    return enrich(store.addItem(jwt.getTokenValue(), request.productId(), quantity));
  }

  @PutMapping("/cart/items/{productId}")
  public EnrichedCart updateItem(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long productId,
      @RequestBody QtyRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar la cantidad");
    }
    if (request.quantity() < 1) {
      return enrich(store.removeItem(jwt.getTokenValue(), productId));
    }
    assertStock(store.product(productId), request.quantity());
    return enrich(store.updateItem(jwt.getTokenValue(), productId, request.quantity()));
  }

  @DeleteMapping("/cart/items/{productId}")
  public EnrichedCart removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
    return enrich(store.removeItem(jwt.getTokenValue(), productId));
  }

  @DeleteMapping("/cart")
  public EnrichedCart clearCart(@AuthenticationPrincipal Jwt jwt) {
    return enrich(store.clearCart(jwt.getTokenValue()));
  }

  // ---- Despacho y pago ------------------------------------------------------

  @PostMapping("/shipping/quote")
  public ShippingDto shipping(@AuthenticationPrincipal Jwt jwt, @RequestBody ShippingRequest request) {
    if (request == null || request.commune() == null || request.commune().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La comuna de despacho es obligatoria");
    }
    EnrichedCart cart = enrich(store.cart(jwt.getTokenValue()));
    return store.shipping(jwt.getTokenValue(), request.region(), request.commune(), cart.total());
  }

  @PostMapping("/payments/intent")
  public PaymentDto payment(@AuthenticationPrincipal Jwt jwt, @RequestBody PaymentRequest request) {
    if (request == null || request.amount() == null || request.amount().signum() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor que cero");
    }
    return store.payment(jwt.getTokenValue(), request.amount(), request.method());
  }

  // ---- Órdenes --------------------------------------------------------------

  @GetMapping("/orders")
  public List<OrderDto> orders(@AuthenticationPrincipal Jwt jwt) {
    List<OrderDto> result = store.orders(jwt.getTokenValue());
    return result == null ? List.of() : result;
  }

  @GetMapping("/orders/{id}")
  public OrderDto order(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return store.order(jwt.getTokenValue(), id);
  }

  /**
   * Checkout orquestado por el BFF:
   * 1. Lee el carrito (400 si está vacío).
   * 2. Cotiza el despacho con el servicio de shipping.
   * 3. Autoriza el pago (subtotal + despacho) con el servicio de pagos.
   * 4. Pide al servicio de órdenes que reserve stock, persista la orden,
   *    vacíe el carrito y notifique.
   */
  @PostMapping("/orders")
  public CheckoutResponse checkout(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody(required = false) CheckoutRequest request) {
    String token = jwt.getTokenValue();
    EnrichedCart cart = enrich(store.cart(token));
    if (cart.items().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
    }
    // Revalida stock actual antes de cobrar.
    for (EnrichedItem line : cart.items()) {
      assertStock(store.product(line.product().id()), line.quantity());
    }

    String region = request != null && request.region() != null && !request.region().isBlank()
        ? request.region().trim()
        : "Metropolitana";
    String commune = request != null && request.commune() != null && !request.commune().isBlank()
        ? request.commune().trim()
        : "Santiago";
    String method = request != null ? request.paymentMethod() : null;

    ShippingDto shipping = store.shipping(token, region, commune, cart.total());
    BigDecimal totalToPay = cart.total().add(shipping.price());
    PaymentDto payment = store.payment(token, totalToPay, method);

    Map<String, Object> checkout = new HashMap<>();
    checkout.put("paymentId", payment.paymentId());
    checkout.put("paymentMethod", payment.method());
    checkout.put("shippingService", shipping.service());
    checkout.put("shippingCost", shipping.price());
    checkout.put("shippingRegion", shipping.region());
    checkout.put("shippingCommune", shipping.commune());

    OrderDto order = store.checkout(token, checkout);
    return new CheckoutResponse(order, payment, shipping);
  }

  // ---- Notificaciones -------------------------------------------------------

  @GetMapping("/notifications")
  public List<NotificationDto> notifications(@AuthenticationPrincipal Jwt jwt) {
    List<NotificationDto> result = store.notifications(jwt.getTokenValue());
    return result == null ? List.of() : result;
  }

  @GetMapping("/notifications/unread-count")
  public Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
    return store.unread(jwt.getTokenValue());
  }

  @PatchMapping("/notifications/read-all")
  public Map<String, Long> markAllRead(@AuthenticationPrincipal Jwt jwt) {
    return store.markAllRead(jwt.getTokenValue());
  }

  @PatchMapping("/notifications/{id}/read")
  public NotificationDto markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return store.markRead(jwt.getTokenValue(), id);
  }

  // ---- Inventario y reseñas -------------------------------------------------

  @GetMapping("/products/{productId}/stock")
  public StockDto stock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
    return store.stock(jwt.getTokenValue(), productId);
  }

  @PostMapping("/products/{productId}/reviews")
  public ReviewDto addReview(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long productId,
      @RequestBody ReviewRequest request) {
    if (request == null || request.rating() < 1 || request.rating() > 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La valoración debe estar entre 1 y 5 estrellas");
    }
    if (request.comment() == null || request.comment().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe un comentario para publicar tu reseña");
    }
    store.product(productId); // 404 si el producto no existe
    return store.addReview(jwt.getTokenValue(), productId, request.rating(), request.comment().trim());
  }

  // ---- Administración (ROLE_ADMIN) ------------------------------------------

  @GetMapping("/admin/inventory")
  public List<AdminStockRow> adminInventory(@AuthenticationPrincipal Jwt jwt) {
    List<ProductDto> products = store.products(null);
    Map<Long, StockDto> stock = new HashMap<>();
    List<StockDto> inventory = store.inventory(jwt.getTokenValue());
    if (inventory != null) {
      inventory.forEach(row -> stock.put(row.productId(), row));
    }
    return products.stream()
        .map(product -> new AdminStockRow(
            product,
            stock.containsKey(product.id()) ? stock.get(product.id()).available() : 0))
        .toList();
  }

  @PostMapping("/admin/products/{productId}/restock")
  public AdminStockRow restock(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long productId,
      @RequestBody QtyRequest request) {
    if (request == null || request.quantity() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad a reponer debe ser positiva");
    }
    ProductDto product = store.restockCatalog(jwt.getTokenValue(), productId, request.quantity());
    StockDto stock = store.releaseInventory(jwt.getTokenValue(), productId, request.quantity());
    return new AdminStockRow(product, stock.available());
  }

  // ---- Helpers --------------------------------------------------------------

  private int quantityInCart(String token, Long productId) {
    CartDto current = store.cart(token);
    if (current == null || current.items() == null) {
      return 0;
    }
    return current.items().stream()
        .filter(item -> productId.equals(item.productId()))
        .mapToInt(CartItemDto::quantity)
        .sum();
  }

  private static void assertStock(ProductDto product, int requested) {
    if (requested > MAX_QTY_PER_LINE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Máximo " + MAX_QTY_PER_LINE + " unidades por producto en una compra");
    }
    if (product.stock() < requested) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Stock insuficiente para " + product.name() + ": quedan " + product.stock() + " unidades");
    }
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

  // ---- Requests / responses -------------------------------------------------

  public record AddItemRequest(Long productId, int quantity) {
  }

  public record QtyRequest(int quantity) {
  }

  public record PaymentRequest(BigDecimal amount, String method) {
  }

  public record ShippingRequest(String region, String commune) {
  }

  public record ReviewRequest(int rating, String comment) {
  }

  public record CheckoutRequest(String region, String commune, String paymentMethod) {
  }

  public record CheckoutResponse(OrderDto order, PaymentDto payment, ShippingDto shipping) {
  }

  public record EnrichedItem(ProductDto product, int quantity, BigDecimal subtotal) {
  }

  public record EnrichedCart(List<EnrichedItem> items, BigDecimal total, int totalItems) {
  }

  public record AdminStockRow(ProductDto product, int reservable) {
  }
}
