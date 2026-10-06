package com.jotirling.repository;

import com.jotirling.entity.ShopOrder;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
  Optional<ShopOrder> findByRequestId(String requestId);
  List<ShopOrder> findByCustomerIdOrderByCreatedAtDesc(Long id);
  List<ShopOrder> findAllByOrderByCreatedAtDesc();
}
