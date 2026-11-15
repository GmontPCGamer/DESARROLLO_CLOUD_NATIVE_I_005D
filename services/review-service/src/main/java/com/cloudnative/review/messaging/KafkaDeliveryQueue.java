package com.cloudnative.review.messaging;

import com.cloudnative.messaging.ReviewCreated;
import com.cloudnative.messaging.PendingKafkaEvents;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KafkaDeliveryQueue {
  private static final Logger log = LoggerFactory.getLogger(KafkaDeliveryQueue.class);
  private final PendingKafkaEvents pending;
  private final KafkaTemplate<String, String> kafka;

  public KafkaDeliveryQueue(KafkaTemplate<String, String> kafka,
      @Value("${APP_DATA_DIR:./.run/data}") String dataDirectory) {
    this.kafka = kafka;
    this.pending = new PendingKafkaEvents(Path.of(dataDirectory, "review-kafka-pending"));
  }

  public void enqueue(String key, String payload) {
    try {
      pending.add(key, payload);
    } catch (IOException failure) {
      throw new IllegalStateException("No se pudo guardar el evento Kafka pendiente", failure);
    }
  }

  @Scheduled(fixedDelayString = "${app.kafka.retry-delay:2000}")
  public void deliver() {
    try {
      for (Path file : pending.pending(20)) {
        var event = pending.read(file);
        var metadata = kafka.send(ReviewCreated.TOPIC, event.key(), event.payload())
            .get(35, TimeUnit.SECONDS).getRecordMetadata();
        pending.acknowledge(file);
        log.info("Kafka confirmó topic={} key={} partition={} offset={}",
            ReviewCreated.TOPIC, event.key(), metadata.partition(), metadata.offset());
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    } catch (IOException | ExecutionException | TimeoutException | RuntimeException failure) {
      log.warn("Quedan eventos Kafka pendientes de reintento en {}", ReviewCreated.TOPIC, failure);
    }
  }
}
