package com.jotirling.repository;

import com.jotirling.entity.Purchase;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
  Optional<Purchase> findByRequestId(String id);
  List<Purchase> findAllByOrderByCreatedAtDesc();
}
