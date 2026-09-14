package com.cloudnative.catalog.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudnative.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findByCategoryIgnoreCase(String category);
}
