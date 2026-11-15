package com.cloudnative.offer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.cloudnative.messaging.ReviewCreated;
import com.cloudnative.offer.domain.OfferProjection;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:h2:mem:offer-flow;DB_CLOSE_DELAY=-1",
        "spring.kafka.listener.concurrency=2"})
@EmbeddedKafka(partitions = 3, topics = {ReviewCreated.TOPIC, ReviewCreated.DLT},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class KafkaFlowTest {
  @DynamicPropertySource
  static void testDatabase(DynamicPropertyRegistry properties) {
    String url = System.getenv("TEST_OFFER_DB_URL");
    if (url != null) {
      properties.add("spring.datasource.url", () -> url);
      properties.add("spring.datasource.username", () -> System.getenv("TEST_OFFER_DB_USER"));
      properties.add("spring.datasource.password", () -> System.getenv("TEST_OFFER_DB_PASSWORD"));
    }
  }

  @Autowired KafkaTemplate<String, String> template;
  @Autowired OfferProjection offers;
  @Autowired JsonMapper mapper;
  @Autowired EmbeddedKafkaBroker broker;
  @Autowired KafkaListenerEndpointRegistry registry;

  @Test void actualKafkaDeliveryDeduplicationAndDeadLetterRecovery() throws Exception {
    await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
      var listener = (org.springframework.kafka.listener.ConcurrentMessageListenerContainer<?, ?>)
          registry.getListenerContainers().iterator().next();
      assertThat(listener.getAssignmentsByClientId().values()).hasSize(2)
          .allSatisfy(partitions -> assertThat(partitions).isNotEmpty());
      assertThat(listener.getAssignedPartitions()).hasSize(3);
    });
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
          assertThat(offers.products()).singleElement().satisfies(offer -> {
            assertThat(offer.productId()).isEqualTo(4321L);
            assertThat(offer.reviewCount()).isEqualTo(2);
          }));
      assertThat(KafkaTestUtils.getSingleRecord(dlt, ReviewCreated.DLT, Duration.ofSeconds(30)).value())
          .isEqualTo("invalid-json");
    }
  }
}
