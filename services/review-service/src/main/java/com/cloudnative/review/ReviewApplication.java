package com.cloudnative.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class ReviewApplication {
  public static void main(String[] args) { SpringApplication.run(ReviewApplication.class, args); }
}
