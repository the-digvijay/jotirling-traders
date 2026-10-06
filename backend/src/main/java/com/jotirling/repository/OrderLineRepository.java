package com.jotirling.repository;

import com.jotirling.entity.OrderLine;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
  List<OrderLine> findByShopOrderIdOrderByProductIdAsc(Long id);
}
