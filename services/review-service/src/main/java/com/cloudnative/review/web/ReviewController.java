package com.cloudnative.review.web;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

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

import com.cloudnative.review.messaging.ReviewEventPublisher;

/**
 * Reseñas de productos. La lectura es pública; publicar requiere sesión.
 * Reglas: rating 1..5, comentario obligatorio (máx. 500 caracteres) y una sola
 * reseña por usuario y producto.
 */
@RestController
@RequestMapping("/api")
public class ReviewController {

  private static final int MAX_COMMENT = 500;

  private final AtomicLong sequence = new AtomicLong();
  private final List<StoredReview> reviews = new CopyOnWriteArrayList<>();
  private final ReviewEventPublisher events;

  public ReviewController(ReviewEventPublisher events) {
    this.events = events;
    seed(1L, "seed-camila", "Camila R.", 5, "La batería y la cámara superaron mis expectativas.", 40);
    seed(1L, "seed-diego", "Diego M.", 4, "Buen equipo, llegó rápido y bien protegido.", 25);
    seed(2L, "seed-valentina", "Valentina P.", 5, "El S Pen y el zoom son otro nivel. Muy contenta.", 30);
    seed(4L, "seed-tomas", "Tomás F.", 5, "Silencioso, liviano y la batería dura todo el día de clases.", 18);
    seed(9L, "seed-javiera", "Javiera L.", 4, "Excelente consola, sólo el tamaño de la caja sorprende.", 12);
    seed(13L, "seed-matias", "Matías G.", 5, "La cancelación de ruido en el metro es impresionante.", 7);
  }

  @GetMapping("/public/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "review-service");
  }

  @GetMapping("/reviews/{productId}")
  public List<Review> list(@PathVariable Long productId) {
    return reviews.stream()
        .filter(review -> review.review().productId().equals(productId))
        .map(StoredReview::review)
        .sorted(Comparator.comparing(Review::createdAt).reversed())
        .toList();
  }

  @PostMapping("/reviews/{productId}")
  public Review create(
      @PathVariable Long productId,
      @RequestBody ReviewRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    if (request == null || request.rating() < 1 || request.rating() > 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La valoración debe estar entre 1 y 5 estrellas");
    }
    if (request.comment() == null || request.comment().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe un comentario para publicar tu reseña");
    }
    if (request.comment().length() > MAX_COMMENT) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "El comentario no puede superar " + MAX_COMMENT + " caracteres");
    }
    String userId = jwt.getSubject();
    boolean alreadyReviewed = reviews.stream()
        .anyMatch(stored -> stored.userId().equals(userId) && stored.review().productId().equals(productId));
    if (alreadyReviewed) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya publicaste una reseña para este producto");
    }

    Review review = new Review(
        sequence.incrementAndGet(),
        productId,
        authorFrom(jwt),
        request.rating(),
        request.comment().trim(),
        Instant.now());
    reviews.add(new StoredReview(userId, review));
    events.published(userId, productId, request.rating());
    return review;
  }

  private static String authorFrom(Jwt jwt) {
    String name = jwt.getClaimAsString("name");
    if (name != null && !name.isBlank()) {
      return name;
    }
    String username = jwt.getClaimAsString("preferred_username");
    if (username != null && !username.isBlank()) {
      // Sólo la parte local del correo para no exponer el dominio completo.
      int at = username.indexOf('@');
      return at > 0 ? username.substring(0, at) : username;
    }
    return "Cliente NexoTech";
  }

  private void seed(Long productId, String userId, String author, int rating, String comment, int daysAgo) {
    Review review = new Review(
        sequence.incrementAndGet(), productId, author, rating, comment, Instant.now().minusSeconds(daysAgo * 86400L));
    reviews.add(new StoredReview(userId, review));
  }

  private record StoredReview(String userId, Review review) {
  }

  public record ReviewRequest(int rating, String comment) {
  }

  public record Review(Long id, Long productId, String author, int rating, String comment, Instant createdAt) {
  }
}
