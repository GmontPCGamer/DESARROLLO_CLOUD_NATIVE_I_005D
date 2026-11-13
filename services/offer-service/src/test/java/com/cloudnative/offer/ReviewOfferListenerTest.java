package com.cloudnative.offer;

import com.cloudnative.messaging.ReviewCreated;
import com.cloudnative.offer.messaging.ReviewOfferListener;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class ReviewOfferListenerTest {
  private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
  private final ReviewOfferListener listener = new ReviewOfferListener(mapper);

  @Test void duplicateEventsAreCountedOnceAndNewEventsWithReusedReviewIdRemainDistinct() {
    var now = Instant.now();
    String first = mapper.writeValueAsString(new ReviewCreated("a", 1L, 42L, 5, now));
    listener.onReview(first);
    listener.onReview(first);
    listener.onReview(mapper.writeValueAsString(new ReviewCreated("b", 1L, 42L, 2, now)));
    assertThat(listener.products().get(42L).reviewCount()).isEqualTo(2);
    assertThat(listener.products().get(42L).positiveReviews()).isEqualTo(1);
  }

  @Test void invalidEventsCannotModifyProjection() {
    assertThatThrownBy(() -> listener.onReview("{broken" )).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> listener.onReview("{\"reviewId\":1,\"productId\":42,\"rating\":6,\"createdAt\":\"2026-11-13T12:00:00Z\"}"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(listener.products()).isEmpty();
  }

  @Test void oldEventsWithoutEventIdAreCompatibleAndIdempotent() {
    String legacy = "{\"reviewId\":1,\"productId\":42,\"rating\":4,\"createdAt\":\"2026-11-13T12:00:00Z\"}";
    listener.onReview(legacy); listener.onReview(legacy);
    assertThat(listener.products().get(42L).reviewCount()).isEqualTo(1);
  }
}
