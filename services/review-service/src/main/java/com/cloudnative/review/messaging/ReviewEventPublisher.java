package com.cloudnative.review.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(ReviewEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public ReviewEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void published(String userId, Long productId, int rating) {
    ReviewNotice notice = new ReviewNotice(
        userId,
        "Reseña publicada",
        "Quedó registrada tu reseña de " + rating + " estrellas para el producto #" + productId + ".",
        "REVIEW");
    try {
      rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.REVIEW_CREATED, notice);
      log.info("Reseña publicada encolada productId={} userId={}", productId, userId);
    } catch (RuntimeException ex) {
      log.warn("La reseña se guardó, pero no se publicó el aviso: {}", ex.getMessage());
    }
  }
}
