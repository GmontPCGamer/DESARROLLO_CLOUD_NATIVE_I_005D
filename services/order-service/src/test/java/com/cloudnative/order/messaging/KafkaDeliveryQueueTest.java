package com.cloudnative.order.messaging;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.cloudnative.messaging.PendingKafkaEvents;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaDeliveryQueueTest {
  @TempDir Path directory;

  @Test
  @SuppressWarnings("unchecked")
  void retainsEventDuringOutageAndDeliversAfterRestart() throws Exception {
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    String topic = "nexotech.order-confirmed";
    String payload = "{\"eventId\":\"demo-order\"}";
    SendResult<String, String> result = mock(SendResult.class);
    when(result.getRecordMetadata()).thenReturn(new RecordMetadata(
        new TopicPartition(topic, 0), 7, 0, 0, 0, 0));
    when(kafka.send(topic, "42", payload))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker offline")))
        .thenReturn(CompletableFuture.completedFuture(result));

    var queue = new KafkaDeliveryQueue(kafka, directory.toString());
    queue.enqueue("42", payload);
    queue.deliver();

    var saved = new PendingKafkaEvents(directory.resolve("order-kafka-pending"));
    assertThat(saved.pending(20)).hasSize(1);

    new KafkaDeliveryQueue(kafka, directory.toString()).deliver();

    assertThat(saved.pending(20)).isEmpty();
    verify(kafka, times(2)).send(topic, "42", payload);
  }
}
