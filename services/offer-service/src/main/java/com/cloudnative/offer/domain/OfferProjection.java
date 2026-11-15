package com.cloudnative.offer.domain;

import com.cloudnative.messaging.ReviewCreated;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OfferProjection {
  private final JdbcTemplate database;

  public OfferProjection(JdbcTemplate database) {
    this.database = database;
  }

  public void record(ReviewCreated event) {
    try {
      database.update("INSERT INTO offer_reviews (event_id, product_id, rating) VALUES (?, ?, ?)",
          event.eventId(), event.productId(), event.rating());
    } catch (DuplicateKeyException duplicate) {
      // Un reenvío del mismo evento ya está registrado, incluso tras un reinicio.
    }
  }

  public List<OfferSignal> products() {
    return database.query("""
        SELECT product_id, COUNT(*) AS reviews,
               SUM(CASE WHEN rating >= 4 THEN 1 ELSE 0 END) AS positive
        FROM offer_reviews
        GROUP BY product_id
        ORDER BY positive DESC, product_id ASC
        """, (row, index) -> new OfferSignal(row.getLong("product_id"),
             row.getInt("reviews"), row.getInt("positive")));
  }

  public record OfferSignal(long productId, int reviewCount, int positiveReviews) {}
}
