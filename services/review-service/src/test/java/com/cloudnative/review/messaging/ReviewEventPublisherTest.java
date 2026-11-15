package com.cloudnative.review.messaging;

import com.cloudnative.messaging.ReviewCreated;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ReviewEventPublisherTest {
  @SuppressWarnings("unchecked")
  @Test void rabbitFailureDoesNotPreventKafkaAndEventContainsNoPersonalData() {
    RabbitTemplate rabbit = mock(RabbitTemplate.class);
    KafkaTemplate<String,String> kafka = mock(KafkaTemplate.class);
    var mapper = JsonMapper.builder().findAndAddModules().build();
    doThrow(new IllegalStateException("rabbit unavailable")).when(rabbit)
        .convertAndSend(anyString(), anyString(), any(ReviewNotice.class));
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka unavailable")));
    var publisher = new ReviewEventPublisher(rabbit, kafka, mapper);
    assertThatCode(() -> publisher.published("private-user", 1L, 42L, 5)).doesNotThrowAnyException();
    var event = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(kafka).send(eq(ReviewCreated.TOPIC), eq("42"), event.capture());
    assertThat(event.getValue()).doesNotContain("private-user");
    var decoded = mapper.readValue(event.getValue(), ReviewCreated.class);
    assertThat(decoded.eventId()).isNotBlank();
    assertThat(decoded.rating()).isEqualTo(5);
  }

  @SuppressWarnings("unchecked")
  @Test void kafkaFailureDoesNotPreventRabbitPublication() {
    RabbitTemplate rabbit = mock(RabbitTemplate.class);
    KafkaTemplate<String,String> kafka = mock(KafkaTemplate.class);
    when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("offline"));
    new ReviewEventPublisher(rabbit, kafka, JsonMapper.builder().findAndAddModules().build())
        .published("user", 1L, 42L, 5);
    verify(rabbit).convertAndSend(eq(RabbitConfig.NOTIFICATIONS_EXCHANGE),
        eq(RabbitConfig.NOTIFICATIONS_ROUTING_KEY), any(ReviewNotice.class));
  }
}
