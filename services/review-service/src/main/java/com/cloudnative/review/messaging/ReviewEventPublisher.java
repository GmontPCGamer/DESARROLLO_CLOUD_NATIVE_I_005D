package com.cloudnative.review.messaging;

import com.cloudnative.messaging.ReviewCreated;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(ReviewEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final JsonMapper objectMapper;

  public ReviewEventPublisher(RabbitTemplate rabbitTemplate, KafkaTemplate<String, String> kafkaTemplate,
      JsonMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  public void published(String userId, Long reviewId, Long productId, int rating) {
    ReviewNotice notice = new ReviewNotice(
        userId,
        "Reseña publicada",
        "Quedó registrada tu reseña de " + rating + " estrellas para el producto #" + productId + ".",
        "REVIEW");
    try {
      rabbitTemplate.convertAndSend(
          RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.NOTIFICATIONS_ROUTING_KEY, notice);
      log.info("Reseña publicada encolada productId={} userId={}", productId, userId);
    } catch (RuntimeException ex) {
      log.warn("La reseña se guardó, pero no se publicó el aviso: {}", ex.getMessage());
    }
    try {
      String event = objectMapper.writeValueAsString(new ReviewCreated(UUID.randomUUID().toString(), reviewId, productId, rating,
          java.time.Instant.now()));
      kafkaTemplate.send(ReviewCreated.TOPIC, productId.toString(), event)
          .whenComplete((result, failure) -> {
            if (failure != null) {
              log.error("Kafka no confirmó la reseña reviewId={} productId={}", reviewId, productId, failure);
            } else {
              log.info("Kafka confirmó reviewId={} partition={} offset={}", reviewId,
                  result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
          });
    } catch (Exception ex) {
      log.warn("La reseña se guardó, pero no se publicó el evento Kafka: {}", ex.getMessage());
    }
  }

}
