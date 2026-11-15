package com.cloudnative.offer.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudnative.offer.messaging.ReviewOfferListener;
import com.cloudnative.offer.messaging.ReviewOfferListener.OfferSignal;

@RestController
@RequestMapping("/api")
public class OfferController {
  private final ReviewOfferListener signals;

  public OfferController(ReviewOfferListener signals) { this.signals = signals; }

  @GetMapping("/public/health")
  public Map<String, String> health() { return Map.of("status", "UP", "service", "offer-service", "queue", "Kafka"); }

  /** Retorna ofertas armadas con señales que el consumidor recibió desde Kafka. */
  @GetMapping("/public/offers")
  public List<Offer> offers() {
    return signals.products().values().stream()
        .filter(signal -> signal.positiveReviews() > 0)
        .sorted((a, b) -> Integer.compare(b.positiveReviews(), a.positiveReviews()))
        .map(signal -> new Offer(signal.productId(), "Destacado por la comunidad",
            signal.positiveReviews() + " reseña(s) positiva(s)", signal.reviewCount()))
        .toList();
  }

  public record Offer(Long productId, String title, String reason, int reviewCount) {}
}
