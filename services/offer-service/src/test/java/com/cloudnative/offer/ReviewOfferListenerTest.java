package com.cloudnative.offer;

import com.cloudnative.messaging.ReviewCreated;
import com.cloudnative.offer.messaging.ReviewOfferListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import com.cloudnative.offer.domain.OfferProjection;
import java.nio.file.Path;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class ReviewOfferListenerTest {
  private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
  @TempDir Path directory;
  private ReviewOfferListener listener;
  private OfferProjection offers;
  private String url;

  @BeforeEach void prepareDatabase() {
    url = "jdbc:h2:file:" + directory.resolve("offers");
    var source = new DriverManagerDataSource(url, "sa", "");
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(source);
    offers = new OfferProjection(new JdbcTemplate(source));
    listener = new ReviewOfferListener(mapper, offers);
  }

  private void accept(String payload) { listener.onReview(payload, 0, 0); }

  @Test void duplicateEventsAreCountedOnceAndNewEventsWithReusedReviewIdRemainDistinct() {
    var now = Instant.now();
    String first = mapper.writeValueAsString(new ReviewCreated("a", 1L, 42L, 5, now));
    accept(first);
    accept(first);
    accept(mapper.writeValueAsString(new ReviewCreated("b", 1L, 42L, 2, now)));
    assertThat(offers.products().get(0).reviewCount()).isEqualTo(2);
    assertThat(offers.products().get(0).positiveReviews()).isEqualTo(1);
    var rebuilt = new OfferProjection(new JdbcTemplate(new DriverManagerDataSource(url, "sa", "")));
    rebuilt.record(mapper.readValue(first, ReviewCreated.class));
    assertThat(rebuilt.products()).isEqualTo(offers.products());
  }

  @Test void invalidEventsCannotModifyProjection() {
    assertThatThrownBy(() -> accept("{broken" )).isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> accept("{\"reviewId\":1,\"productId\":42,\"rating\":6,\"createdAt\":\"2026-11-13T12:00:00Z\"}"))
        .isInstanceOf(RuntimeException.class);
    assertThat(offers.products()).isEmpty();
  }

  @Test void oldEventsWithoutEventIdAreCompatibleAndIdempotent() {
    String legacy = "{\"reviewId\":1,\"productId\":42,\"rating\":4,\"createdAt\":\"2026-11-13T12:00:00Z\"}";
    accept(legacy); accept(legacy);
    assertThat(offers.products().get(0).reviewCount()).isEqualTo(1);
  }
}
