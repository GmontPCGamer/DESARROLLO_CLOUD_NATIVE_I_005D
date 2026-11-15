package com.cloudnative.offer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class OfferApplication {
  public static void main(String[] args) { SpringApplication.run(OfferApplication.class, args); }
}
