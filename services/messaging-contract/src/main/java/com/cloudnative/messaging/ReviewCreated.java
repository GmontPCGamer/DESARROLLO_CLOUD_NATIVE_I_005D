package com.cloudnative.messaging;

import java.time.Instant;

/** Evento v1 sin datos personales; la clave Kafka es productId. */
public record ReviewCreated(String eventId, Long reviewId, Long productId, int rating, Instant createdAt) {
  public static final String TOPIC = "nexotech.review-created";
  public static final String DLT = TOPIC + ".DLT";

  public ReviewCreated {
    if (reviewId == null || reviewId <= 0 || productId == null || productId <= 0
        || rating < 1 || rating > 5 || createdAt == null) {
      throw new IllegalArgumentException("Evento de reseña incompleto o fuera de rango");
    }
    // Compatibilidad con eventos existentes, anteriores al campo eventId.
    if (eventId == null || eventId.isBlank()) eventId = reviewId + ":" + productId + ":" + createdAt;
  }
}
