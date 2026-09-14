package com.cloudnative.order.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudnative.order.domain.CustomerOrder;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
  List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(String userId);

  Optional<CustomerOrder> findByIdAndUserId(Long id, String userId);
}
