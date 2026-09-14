package com.cloudnative.order.client;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StoreClients {

  private final RestClient catalog;
  private final RestClient cart;
  private final RestClient notifications;

  public StoreClients(
      RestClient.Builder builder,
      @Value("${app.services.catalog}") String catalogUrl,
      @Value("${app.services.cart}") String cartUrl,
      @Value("${app.services.notifications}") String notificationsUrl) {
    this.catalog = builder.clone().baseUrl(catalogUrl).build();
    this.cart = builder.clone().baseUrl(cartUrl).build();
    this.notifications = builder.clone().baseUrl(notificationsUrl).build();
  }

  public ProductDto product(Long id) {
    try {
      ProductDto product = catalog.get().uri("/api/products/{id}", id).retrieve().body(ProductDto.class);
      if (product == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto no disponible");
      }
      return product;
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no disponible");
    }
  }

  public ProductDto reserve(String token, Long id, int quantity) {
    try {
      return catalog.post()
          .uri("/api/products/{id}/reserve", id)
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .body(new ReserveRequest(quantity))
          .retrieve()
          .body(ProductDto.class);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay stock suficiente para el pedido");
    }
  }

  public CartDto cart(String token) {
    try {
      CartDto body = cart.get()
          .uri("/api/cart")
          .header("Authorization", "Bearer " + token)
          .retrieve()
          .body(CartDto.class);
      if (body == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrito vacío");
      }
      return body;
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Carrito no disponible");
    }
  }

  public void clearCart(String token) {
    cart.delete()
        .uri("/api/cart")
        .header("Authorization", "Bearer " + token)
        .retrieve()
        .toBodilessEntity();
  }

  public void notify(String token, String title, String message) {
    try {
      notifications.post()
          .uri("/api/notifications")
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .body(new CreateNotification(title, message, "ORDER"))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ignored) {
      // La compra no se revierte si falla la notificación.
    }
  }

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

  public record CreateNotification(String title, String message, String type) {
  }

  public record ReserveRequest(int quantity) {
  }
}
