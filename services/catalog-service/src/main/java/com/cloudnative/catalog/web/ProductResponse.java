package com.cloudnative.catalog.web;

import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String name,
    String description,
    String category,
    String brand,
    BigDecimal price,
    String imageUrl,
    int stock) {
}
