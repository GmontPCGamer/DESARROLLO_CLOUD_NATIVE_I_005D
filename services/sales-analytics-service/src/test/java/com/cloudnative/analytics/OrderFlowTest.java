package com.cloudnative.analytics;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import com.cloudnative.analytics.domain.SalesProjection;
import com.cloudnative.messaging.OrderConfirmed;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"spring.datasource.url=jdbc:h2:mem:analytics-test;DB_CLOSE_DELAY=-1"})
@EmbeddedKafka(partitions=3,topics={OrderConfirmed.TOPIC,OrderConfirmed.DLT},bootstrapServersProperty="spring.kafka.bootstrap-servers")
@DirtiesContext
class OrderFlowTest {
  @Autowired KafkaTemplate<String,String> template;
  @Autowired EmbeddedKafkaBroker broker;
  @Autowired JsonMapper mapper;
  @Autowired SalesProjection sales;
  @Test void twoPurchasesDuplicatesAndInvalidMessageAreHandled()throws Exception{
    var first=new OrderConfirmed("order-a",1L,new BigDecimal("1000"),"CLP",Instant.now(),List.of(new OrderConfirmed.Item(1L,2,new BigDecimal("500"))));
    var second=new OrderConfirmed("order-b",2L,new BigDecimal("750"),"CLP",Instant.now(),List.of(new OrderConfirmed.Item(2L,1,new BigDecimal("750"))));
    try(Consumer<String,String> dlt=new DefaultKafkaConsumerFactory<>(KafkaTestUtils.consumerProps("sales-dlt","false",broker),new StringDeserializer(),new StringDeserializer()).createConsumer()){
      broker.consumeFromAnEmbeddedTopic(dlt,OrderConfirmed.DLT);
      template.send(OrderConfirmed.TOPIC,"1",mapper.writeValueAsString(first)).get();
      template.send(OrderConfirmed.TOPIC,"1",mapper.writeValueAsString(first)).get();
      template.send(OrderConfirmed.TOPIC,"1","{invalid").get();
      template.send(OrderConfirmed.TOPIC,"2",mapper.writeValueAsString(second)).get();
      await().atMost(Duration.ofSeconds(30)).untilAsserted(()->{
        assertThat(sales.summary().orderCount()).isEqualTo(2);
        assertThat(sales.summary().revenue()).isEqualByComparingTo("1750");
        assertThat(sales.summary().units()).isEqualTo(3);
      });
      assertThat(KafkaTestUtils.getSingleRecord(dlt,OrderConfirmed.DLT,Duration.ofSeconds(30)).value()).isEqualTo("{invalid");
      assertThat(sales.products()).hasSize(2);
    }
  }
  @Test void malformedBusinessDtoIsRejected(){
    assertThatThrownBy(()->new OrderConfirmed("bad",1L,new BigDecimal("-1"),"CLP",Instant.now(),List.of())).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(()->new OrderConfirmed.Item(1L,0,BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
  }
}
