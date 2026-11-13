package com.cloudnative.offer.messaging;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import com.cloudnative.messaging.ReviewCreated;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/** Actualiza el menú de promociones a partir del flujo de reseñas Kafka. */
@Component
public class ReviewOfferListener {
  private static final Logger log = LoggerFactory.getLogger(ReviewOfferListener.class);
  private final Map<Long, OfferSignal> products = new ConcurrentHashMap<>();
  private final Set<String> processed = new HashSet<>();
  private final JsonMapper objectMapper;

  public ReviewOfferListener(JsonMapper objectMapper) { this.objectMapper = objectMapper; }

  @KafkaListener(topics = ReviewCreated.TOPIC)
  public synchronized void onReview(String json) {
    try {
      ReviewCreated event = objectMapper.readValue(json, ReviewCreated.class);
      if (processed.contains(event.eventId())) return;
      products.compute(event.productId(), (id, previous) -> {
        int count = previous == null ? 1 : previous.reviewCount() + 1;
        int positive = previous == null ? (event.rating() >= 4 ? 1 : 0)
            : previous.positiveReviews() + (event.rating() >= 4 ? 1 : 0);
        return new OfferSignal(id, count, positive, event.rating());
      });
      processed.add(event.eventId());
      log.info("Señal de oferta actualizada desde Kafka para producto {}", event.productId());
    } catch (Exception ex) {
      log.error("No se pudo procesar evento de reseña Kafka", ex);
      throw new IllegalArgumentException("Evento Kafka de reseña inválido", ex);
    }
  }

  public Map<Long, OfferSignal> products() { return Map.copyOf(products); }

  public record OfferSignal(Long productId, int reviewCount, int positiveReviews, int latestRating) {}
}
