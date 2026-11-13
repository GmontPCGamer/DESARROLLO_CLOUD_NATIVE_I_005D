package com.cloudnative.order.messaging;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import com.cloudnative.messaging.OrderConfirmed;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OrderKafkaPublisherTest {
 @SuppressWarnings("unchecked") @Test void publishesTypedEventUsingOrderAsKeyWithoutPersonalData(){
  KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
  when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")));
  var mapper=JsonMapper.builder().findAndAddModules().build();
  var event=new OrderConfirmed("id",1L,BigDecimal.TEN,"CLP",Instant.now(),List.of(new OrderConfirmed.Item(2L,1,BigDecimal.TEN)));
  new OrderKafkaPublisher(kafka,mapper).publish(event);
  var payload=org.mockito.ArgumentCaptor.forClass(String.class);
  verify(kafka).send(eq(OrderConfirmed.TOPIC),eq("1"),payload.capture());
  assertThat(mapper.readValue(payload.getValue(),OrderConfirmed.class)).isEqualTo(event);
  assertThat(payload.getValue()).doesNotContain("userId","paymentId","shipping");
 }
}
