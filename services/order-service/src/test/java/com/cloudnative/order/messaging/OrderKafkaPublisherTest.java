package com.cloudnative.order.messaging;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.cloudnative.messaging.OrderConfirmed;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OrderKafkaPublisherTest {
 @SuppressWarnings("unchecked") @Test void publishesTypedEventUsingOrderAsKeyWithoutPersonalData(){
  KafkaDeliveryQueue kafka=mock(KafkaDeliveryQueue.class);
  var mapper=JsonMapper.builder().findAndAddModules().build();
  var event=new OrderConfirmed("id",1L,BigDecimal.TEN,"CLP",Instant.now(),List.of(new OrderConfirmed.Item(2L,1,BigDecimal.TEN)));
  new OrderKafkaPublisher(kafka,mapper).publish(event);
  var payload=org.mockito.ArgumentCaptor.forClass(String.class);
  verify(kafka).enqueue(eq("1"),payload.capture());
  assertThat(mapper.readValue(payload.getValue(),OrderConfirmed.class)).isEqualTo(event);
  assertThat(payload.getValue()).doesNotContain("userId","paymentId","shipping");
 }
}
