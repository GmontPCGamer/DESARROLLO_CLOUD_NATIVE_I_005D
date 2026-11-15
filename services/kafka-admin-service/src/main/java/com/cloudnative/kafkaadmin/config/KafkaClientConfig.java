package com.cloudnative.kafkaadmin.config;
import java.util.Map;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class KafkaClientConfig {
  @Bean(destroyMethod="close") Admin admin(@Value("${spring.kafka.bootstrap-servers}") String servers) {
    return Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,servers,
        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,10000,AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,10000));
  }
}
