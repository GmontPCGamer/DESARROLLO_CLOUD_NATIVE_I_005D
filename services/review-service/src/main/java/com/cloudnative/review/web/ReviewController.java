package com.cloudnative.review.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewController {
  private final List<Review> reviews = new CopyOnWriteArrayList<>(List.of(new Review(1L, "Camila R.", 5, "La batería y la cámara superaron mis expectativas.", Instant.now()), new Review(1L, "Diego M.", 4, "Buen equipo, llegó rápido y bien protegido.", Instant.now())));
  @GetMapping("/public/health") public Map<String, String> health() { return Map.of("status", "UP", "service", "review-service"); }
  @GetMapping("/reviews/{productId}") public List<Review> list(@PathVariable Long productId) { return reviews.stream().filter(review -> review.productId().equals(productId)).toList(); }
  @PostMapping("/reviews/{productId}") public Review create(@PathVariable Long productId, @RequestBody ReviewRequest request, @AuthenticationPrincipal Jwt jwt) {
    Review review = new Review(productId, jwt.getClaimAsString("name") == null ? "Cliente Nexo" : jwt.getClaimAsString("name"), request.rating(), request.comment(), Instant.now()); reviews.add(review); return review;
  }
  public record ReviewRequest(int rating, String comment) {}
  public record Review(Long productId, String author, int rating, String comment, Instant createdAt) {}
}
