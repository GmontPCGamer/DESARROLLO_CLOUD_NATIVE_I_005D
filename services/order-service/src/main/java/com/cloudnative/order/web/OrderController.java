package com.cloudnative.order.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

import com.cloudnative.order.client.StoreClients;
import com.cloudnative.order.client.StoreClients.CartDto;
import com.cloudnative.order.client.StoreClients.CartItemDto;
import com.cloudnative.order.client.StoreClients.ProductDto;
import com.cloudnative.order.domain.CustomerOrder;
import com.cloudnative.order.domain.OrderLine;
import com.cloudnative.order.repo.CustomerOrderRepository;

@RestController
@RequestMapping("/api")
public class OrderController {

  private final CustomerOrderRepository orders;
  private final StoreClients clients;

  public OrderController(CustomerOrderRepository orders, StoreClients clients) {
    this.orders = orders;
    this.clients = clients;
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "order-service");
  }

  @GetMapping("/orders")
  public List<OrderResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return orders.findByUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/orders/{id}")
  public OrderResponse byId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return orders.findByIdAndUserId(id, jwt.getSubject())
        .map(this::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
  }

  /**
   * Checkout. Pasos:
   * 1. Lee el carrito del usuario.
   * 2. Consulta precio y nombre actual de cada producto.
   * 3. Reserva stock en inventario y descuenta el catálogo (con compensación si algo falla).
   * 4. Persiste la orden con despacho y pago ya autorizados por el BFF.
   * 5. Vacía el carrito y genera una notificación.
   */
  @PostMapping("/orders")
  public OrderResponse checkout(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody(required = false) CheckoutRequest request) {
    String token = jwt.getTokenValue();
    CartDto cart = clients.cart(token);
    if (cart.items() == null || cart.items().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
    }

    CustomerOrder order = new CustomerOrder();
    order.setUserId(jwt.getSubject());
    order.setStatus("CONFIRMADA");
    order.setCreatedAt(Instant.now());

    BigDecimal subtotal = BigDecimal.ZERO;
    for (CartItemDto item : cart.items()) {
      if (item.quantity() < 1) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad inválida en el carrito");
      }
      ProductDto product = clients.product(item.productId());
      if (product.stock() < item.quantity()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Sin stock suficiente para " + product.name() + " (quedan " + product.stock() + ")");
      }
      OrderLine line = new OrderLine();
      line.setProductId(product.id());
      line.setProductName(product.name());
      line.setUnitPrice(product.price());
      line.setQuantity(item.quantity());
      order.addLine(line);
      subtotal = subtotal.add(product.price().multiply(BigDecimal.valueOf(item.quantity())));
    }

    reserveStockWithCompensation(token, cart.items());

    BigDecimal shippingCost = request != null && request.shippingCost() != null
        ? request.shippingCost().max(BigDecimal.ZERO)
        : BigDecimal.ZERO;
    order.setSubtotal(subtotal);
    order.setShippingCost(shippingCost);
    order.setTotal(subtotal.add(shippingCost));
    if (request != null) {
      order.setShippingService(request.shippingService());
      order.setShippingRegion(request.shippingRegion());
      order.setShippingCommune(request.shippingCommune());
      order.setPaymentId(request.paymentId());
      order.setPaymentMethod(request.paymentMethod());
    }

    CustomerOrder saved = orders.save(order);

    clients.clearCart(token);
    clients.notify(token, "Compra confirmada", notificationMessage(saved));

    return toResponse(saved);
  }

  /**
   * Reserva en inventario y catálogo ítem por ítem. Si una reserva falla,
   * libera todo lo reservado hasta ese momento y propaga el error (409/503).
   */
  private void reserveStockWithCompensation(String token, List<CartItemDto> items) {
    List<CartItemDto> inventoryReserved = new ArrayList<>();
    List<CartItemDto> catalogReserved = new ArrayList<>();
    try {
      for (CartItemDto item : items) {
        clients.reserveInventory(token, item.productId(), item.quantity());
        inventoryReserved.add(item);
        clients.reserveCatalog(token, item.productId(), item.quantity());
        catalogReserved.add(item);
      }
    } catch (RuntimeException ex) {
      for (CartItemDto item : inventoryReserved) {
        clients.releaseInventory(token, item.productId(), item.quantity());
      }
      for (CartItemDto item : catalogReserved) {
        clients.restockCatalog(token, item.productId(), item.quantity());
      }
      throw ex;
    }
  }

  private static String notificationMessage(CustomerOrder order) {
    NumberFormat clp = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL"));
    clp.setMaximumFractionDigits(0);
    StringBuilder message = new StringBuilder()
        .append("Tu pedido #").append(order.getId())
        .append(" por ").append(clp.format(order.getTotal()))
        .append(" fue confirmado");
    if (order.getShippingCommune() != null && !order.getShippingCommune().isBlank()) {
      message.append(". Despacho ")
          .append(order.getShippingService() != null ? order.getShippingService() : "")
          .append(" a ").append(order.getShippingCommune());
    }
    if (order.getPaymentId() != null) {
      message.append(". Pago autorizado (").append(order.getPaymentId(), 0, 8).append("…)");
    }
    return message.append('.').toString();
  }

  private OrderResponse toResponse(CustomerOrder order) {
    List<OrderLineResponse> lines = order.getLines().stream()
        .map(line -> new OrderLineResponse(
            line.getProductId(),
            line.getProductName(),
            line.getUnitPrice(),
            line.getQuantity()))
        .toList();
    return new OrderResponse(
        order.getId(),
        order.getStatus(),
        order.getSubtotal(),
        order.getShippingCost(),
        order.getTotal(),
        order.getShippingService(),
        order.getShippingRegion(),
        order.getShippingCommune(),
        order.getPaymentId(),
        order.getPaymentMethod(),
        order.getCreatedAt(),
        lines);
  }

  public record CheckoutRequest(
      String paymentId,
      String paymentMethod,
      String shippingService,
      BigDecimal shippingCost,
      String shippingRegion,
      String shippingCommune) {
  }

  public record OrderLineResponse(Long productId, String productName, BigDecimal unitPrice, int quantity) {
  }

  public record OrderResponse(
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
      List<OrderLineResponse> lines) {
  }
}
