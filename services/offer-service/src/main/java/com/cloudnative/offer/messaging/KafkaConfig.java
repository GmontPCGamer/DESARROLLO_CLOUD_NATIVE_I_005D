package com.cloudnative.offer.messaging;

import com.cloudnative.messaging.ReviewCreated;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
  @Value("${app.kafka.replicas:1}") private int replicas;
  @Bean NewTopic businessTopic() {
    return TopicBuilder.name(ReviewCreated.TOPIC).partitions(3).replicas(replicas)
        .config("cleanup.policy","delete").config("retention.ms","-1")
        .config("retention.bytes","-1").build();
  }
  @Bean NewTopic deadLetterTopic() {
    return TopicBuilder.name(ReviewCreated.DLT).partitions(3).replicas(replicas)
        .config("cleanup.policy","delete").config("retention.ms","1209600000").build();
  }

  @Bean
  DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String,String> template) {
    var recoverer=new DeadLetterPublishingRecoverer(template,
        (record,error)->new TopicPartition(ReviewCreated.DLT,record.partition()));
    recoverer.setFailIfSendResultIsError(true);
    return new DefaultErrorHandler(recoverer,new FixedBackOff(1000L,2L));
  }

}
