package com.cloudnative.offer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.cloudnative.messaging.ReviewCreated;
import com.cloudnative.offer.messaging.ReviewOfferListener;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 3, topics = {ReviewCreated.TOPIC, ReviewCreated.DLT},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class KafkaFlowTest {
  @Autowired KafkaTemplate<String, String> template;
  @Autowired ReviewOfferListener listener;
  @Autowired JsonMapper mapper;
  @Autowired EmbeddedKafkaBroker broker;

  @Test void actualKafkaDeliveryDeduplicationAndDeadLetterRecovery() throws Exception {
    String event = mapper.writeValueAsString(new ReviewCreated("integration", 1L, 4321L, 5, Instant.now()));
    try (Consumer<String, String> dlt = new DefaultKafkaConsumerFactory<>(
        KafkaTestUtils.consumerProps("dlt-test-" + UUID.randomUUID(), "false", broker),
        new StringDeserializer(), new StringDeserializer()).createConsumer()) {
      broker.consumeFromAnEmbeddedTopic(dlt, ReviewCreated.DLT);
      template.send(ReviewCreated.TOPIC, "4321", event).get();
      template.send(ReviewCreated.TOPIC, "4321", event).get();
      template.send(ReviewCreated.TOPIC, "4321", "invalid-json").get();
      String next = mapper.writeValueAsString(new ReviewCreated("integration-2", 2L, 4321L, 4, Instant.now()));
      template.send(ReviewCreated.TOPIC, "4321", next).get();
      await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
          assertThat(listener.products().get(4321L).reviewCount()).isEqualTo(2));
      assertThat(KafkaTestUtils.getSingleRecord(dlt, ReviewCreated.DLT, Duration.ofSeconds(30)).value())
          .isEqualTo("invalid-json");
    }
  }
}
