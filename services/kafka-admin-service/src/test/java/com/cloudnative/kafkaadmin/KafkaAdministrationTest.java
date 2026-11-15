package com.cloudnative.kafkaadmin;
import java.util.*;
import com.cloudnative.kafkaadmin.service.TopicAdministration;
import com.cloudnative.kafkaadmin.service.TopicAdministration.*;
import com.cloudnative.messaging.ReviewCreated;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions=3,bootstrapServersProperty="spring.kafka.bootstrap-servers")
@DirtiesContext
class KafkaAdministrationTest {
 @Autowired TopicAdministration admin;
 @LocalServerPort int port;
 @Test void topicLifecycleIncludesConfigurationAndPartitions(){
   admin.create(new CreateTopic("demo.lifecycle",1,1,Map.of("retention.ms","3600000","cleanup.policy","delete")));
   assertThat(admin.topics()).anyMatch(t->t.name().equals("demo.lifecycle")&&t.partitions()==1);
   admin.update("demo.lifecycle",new UpdateTopic(2,Map.of("retention.ms","7200000")));
   assertThat(admin.config("demo.lifecycle")).containsEntry("retention.ms","7200000");
   assertThat(admin.topics()).anyMatch(t->t.name().equals("demo.lifecycle")&&t.partitions()==2);
   assertThat(admin.groups()).isNotNull();
   admin.delete("demo.lifecycle");
   await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(admin.topics()).noneMatch(t->t.name().equals("demo.lifecycle")));
 }
 @Test void protectsBusinessTopicsAndRejectsUnsafeConfiguration(){
   assertThatThrownBy(()->admin.delete(ReviewCreated.TOPIC)).hasMessageContaining("protegidos");
   assertThatThrownBy(()->TopicAdministration.validateName("../bad")).hasMessageContaining("inválido");
   assertThatThrownBy(()->TopicAdministration.validateConfig(Map.of("retention.ms","-2"))).hasMessageContaining("fuera de rango");
   assertThatThrownBy(()->TopicAdministration.validateConfig(Map.of("unknown","123"))).hasMessageContaining("no permitida");
 }
 @Test void apiRequiresAuthenticatedAdministrator()throws Exception{
   var client=RestClient.create("http://localhost:"+port);
   assertThatThrownBy(()->client.get().uri("/api/kafka/topics").retrieve().toBodilessEntity())
       .isInstanceOfSatisfying(HttpClientErrorException.class,e->assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
   assertThatThrownBy(()->client.get().uri("/api/kafka/topics").header("Authorization","Bearer "+token("USER")).retrieve().toBodilessEntity())
       .isInstanceOfSatisfying(HttpClientErrorException.class,e->assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
   assertThat(client.get().uri("/api/kafka/topics").header("Authorization","Bearer "+token("ADMIN")).retrieve().toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
 }
 private static String token(String role)throws Exception{
   var b=Base64.getUrlEncoder().withoutPadding();
   long exp=java.time.Instant.now().getEpochSecond()+120;
   String value=b.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))+"."+
       b.encodeToString(("{\"sub\":\"test\",\"exp\":"+exp+",\"roles\":[\""+role+"\"]}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
   var mac=javax.crypto.Mac.getInstance("HmacSHA256");
   mac.init(new javax.crypto.spec.SecretKeySpec(Base64.getDecoder().decode("c2VjcmV0by1sb2NhbC1wYXJhLWRlc2Fycm9sbG8tZGVtby1jbGF2ZS0xMjM0NTY3ODkwYWJjZGVm"),"HmacSHA256"));
   return value+"."+b.encodeToString(mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
 }
}
