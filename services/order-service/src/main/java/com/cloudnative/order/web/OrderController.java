package com.cloudnative.order.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @PostMapping("/orders")
  @Transactional
  public OrderResponse checkout(@AuthenticationPrincipal Jwt jwt) {
    String token = jwt.getTokenValue();
    CartDto cart = clients.cart(token);
    if (cart.items() == null || cart.items().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
    }

    CustomerOrder order = new CustomerOrder();
    order.setUserId(jwt.getSubject());
    order.setStatus("CONFIRMADA");
    order.setCreatedAt(Instant.now());

    BigDecimal total = BigDecimal.ZERO;
    List<CartItemDto> requestedItems = new ArrayList<>();
    for (CartItemDto item : cart.items()) {
      ProductDto product = clients.product(item.productId());
      if (product.stock() < item.quantity()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Sin stock suficiente para " + product.name());
      }
      OrderLine line = new OrderLine();
      line.setProductId(product.id());
      line.setProductName(product.name());
      line.setUnitPrice(product.price());
      line.setQuantity(item.quantity());
      order.addLine(line);
      requestedItems.add(item);
      total = total.add(product.price().multiply(BigDecimal.valueOf(item.quantity())));
    }
    for (CartItemDto item : requestedItems) {
      clients.reserve(token, item.productId(), item.quantity());
    }
    order.setTotal(total);
    CustomerOrder saved = orders.save(order);

    clients.clearCart(token);
    clients.notify(
        token,
        "Compra confirmada",
        "Tu pedido #" + saved.getId() + " por " + saved.getTotal() + " CLP fue registrado.");

    return toResponse(saved);
  }

  private OrderResponse toResponse(CustomerOrder order) {
    List<OrderLineResponse> lines = order.getLines().stream()
        .map(line -> new OrderLineResponse(
            line.getProductId(),
            line.getProductName(),
            line.getUnitPrice(),
            line.getQuantity()))
        .toList();
    return new OrderResponse(order.getId(), order.getStatus(), order.getTotal(), order.getCreatedAt(), lines);
  }

  public record OrderLineResponse(Long productId, String productName, BigDecimal unitPrice, int quantity) {
  }

  public record OrderResponse(
      Long id,
      String status,
      BigDecimal total,
      Instant createdAt,
      List<OrderLineResponse> lines) {
  }
}
