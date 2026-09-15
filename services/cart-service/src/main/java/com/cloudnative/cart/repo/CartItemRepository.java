package com.cloudnative.cart.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudnative.cart.domain.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  List<CartItem> findByUserIdOrderByIdAsc(String userId);

  Optional<CartItem> findByUserIdAndProductId(String userId, Long productId);

  void deleteByUserId(String userId);

  void deleteByUserIdAndProductId(String userId, Long productId);
}
