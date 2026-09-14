package com.cloudnative.catalog.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cloudnative.catalog.domain.Product;
import com.cloudnative.catalog.repo.ProductRepository;

@RestController
@RequestMapping("/api")
public class ProductController {

  private final ProductRepository products;

  public ProductController(ProductRepository products) {
    this.products = products;
  }

  @GetMapping("/products")
  public List<ProductResponse> list(@RequestParam(required = false) String category) {
    List<Product> result = (category == null || category.isBlank())
        ? products.findAll()
        : products.findByCategoryIgnoreCase(category);
    return result.stream().map(this::toResponse).toList();
  }

  @GetMapping("/products/{id}")
  public ProductResponse byId(@PathVariable Long id) {
    return products.findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
  }

  @PostMapping("/products/{id}/reserve")
  public ProductResponse reserve(@PathVariable Long id, @RequestBody ReserveRequest request) {
    if (request.quantity() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser positiva");
    }
    Product product = products.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    if (product.getStock() < request.quantity()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para " + product.getName());
    }
    product.setStock(product.getStock() - request.quantity());
    return toResponse(products.save(product));
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "catalog-service");
  }

  private ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getCategory(),
        product.getBrand(),
        product.getPrice(),
        product.getImageUrl(),
        product.getStock());
  }

  public record ReserveRequest(int quantity) {
  }
}
