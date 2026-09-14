package com.cloudnative.notification.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudnative.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

  long countByUserIdAndReadFalse(String userId);
}
