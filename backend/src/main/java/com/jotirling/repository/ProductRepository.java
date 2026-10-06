package com.jotirling.repository;

import com.jotirling.entity.Product;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findAllByOrderByIdAsc();
}
