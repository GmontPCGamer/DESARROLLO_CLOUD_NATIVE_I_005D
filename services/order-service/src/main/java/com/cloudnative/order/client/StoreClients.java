package com.cloudnative.order.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Clientes HTTP hacia los demás microservicios. Todas las llamadas reenvían
 * el mismo Bearer token del usuario para que cada servicio vuelva a validar
 * el JWT (defensa en profundidad).
 */
@Component
public class StoreClients {

  private static final Logger log = LoggerFactory.getLogger(StoreClients.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final RestClient catalog;
  private final RestClient cart;
  private final RestClient inventory;

  public StoreClients(
      RestClient.Builder builder,
      @Value("${app.services.catalog}") String catalogUrl,
      @Value("${app.services.cart}") String cartUrl,
      @Value("${app.services.inventory}") String inventoryUrl) {
    this.catalog = builder.clone().baseUrl(catalogUrl).build();
    this.cart = builder.clone().baseUrl(cartUrl).build();
    this.inventory = builder.clone().baseUrl(inventoryUrl).build();
  }

  // ---- Catálogo -----------------------------------------------------------

  public ProductDto product(Long id) {
    ProductDto product = call("Catálogo", () -> catalog.get()
        .uri("/api/products/{id}", id)
        .retrieve()
        .body(ProductDto.class));
    if (product == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto " + id + " no disponible");
    }
    return product;
  }

  public void reserveCatalog(String token, Long id, int quantity) {
    call("Catálogo", () -> catalog.post()
        .uri("/api/products/{id}/reserve", id)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(new QuantityRequest(quantity))
        .retrieve()
        .toBodilessEntity());
  }

  public void restockCatalog(String token, Long id, int quantity) {
    compensate("Catálogo", () -> catalog.post()
        .uri("/api/products/{id}/restock", id)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(new QuantityRequest(quantity))
        .retrieve()
        .toBodilessEntity());
  }

  // ---- Inventario ---------------------------------------------------------

  public void reserveInventory(String token, Long id, int quantity) {
    call("Inventario", () -> inventory.post()
        .uri("/api/inventory/{id}/reserve", id)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(new QuantityRequest(quantity))
        .retrieve()
        .toBodilessEntity());
  }

  public void releaseInventory(String token, Long id, int quantity) {
    compensate("Inventario", () -> inventory.post()
        .uri("/api/inventory/{id}/release", id)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(new QuantityRequest(quantity))
        .retrieve()
        .toBodilessEntity());
  }

  // ---- Carrito ------------------------------------------------------------

  public CartDto cart(String token) {
    CartDto body = call("Carrito", () -> cart.get()
        .uri("/api/cart")
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(CartDto.class));
    if (body == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
    }
    return body;
  }

  public void clearCart(String token) {
    compensate("Carrito", () -> cart.delete()
        .uri("/api/cart")
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .toBodilessEntity());
  }

  // ---- Utilidades ---------------------------------------------------------

  /**
   * Ejecuta la llamada y traduce los errores del servicio remoto al mismo
   * código HTTP y mensaje, o a 503 si el servicio no responde.
   */
  private static <T> T call(String serviceName, Supplier<T> action) {
    try {
      return action.get();
    } catch (RestClientResponseException ex) {
      HttpStatusCode status = ex.getStatusCode();
      String detail = detailFrom(ex.getResponseBodyAsString());
      if (status.value() == 401 || status.value() == 403) {
        // El token del usuario no fue aceptado por el servicio interno.
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            serviceName + " rechazó la credencial del usuario (" + status.value() + ")");
      }
      throw new ResponseStatusException(status,
          detail != null ? detail : serviceName + " respondió " + status.value());
    } catch (ResourceAccessException ex) {
      log.warn("{} no disponible: {}", serviceName, ex.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, serviceName + " no disponible");
    }
  }

  /** Acciones de compensación o secundarias: nunca deben tumbar la operación principal. */
  private static void compensate(String serviceName, Supplier<?> action) {
    try {
      action.get();
    } catch (RuntimeException ex) {
      log.warn("Acción secundaria en {} falló: {}", serviceName, ex.getMessage());
    }
  }

  private static String detailFrom(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      ProblemDetail problem = JSON.readValue(body, ProblemDetail.class);
      return problem.getDetail();
    } catch (Exception ignored) {
      return null;
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

  public record QuantityRequest(int quantity) {
  }
}
