package com.cloudnative.offer.messaging;

import com.cloudnative.messaging.ReviewCreated;
import com.cloudnative.offer.domain.OfferProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ReviewOfferListener {
  private static final Logger log = LoggerFactory.getLogger(ReviewOfferListener.class);
  private final JsonMapper mapper;
  private final OfferProjection offers;

  public ReviewOfferListener(JsonMapper mapper, OfferProjection offers) {
    this.mapper = mapper;
    this.offers = offers;
  }

  @KafkaListener(topics = ReviewCreated.TOPIC)
  public void onReview(String payload, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    try {
      offers.record(mapper.readValue(payload, ReviewCreated.class));
    } catch (RuntimeException failure) {
      log.warn("No se pudo procesar la reseña: topic={} partition={} offset={}",
          ReviewCreated.TOPIC, partition, offset, failure);
      throw failure;
    }
  }
}
