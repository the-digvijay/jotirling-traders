package com.jotirling.repository;

import com.jotirling.entity.ShopOrder;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
  List<ShopOrder> findByDeliveryDate(LocalDate date);
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select o from ShopOrder o where o.id = :id")
  Optional<ShopOrder> lockById(@org.springframework.data.repository.query.Param("id") Long id);
  Optional<ShopOrder> findByRequestId(String requestId);
  List<ShopOrder> findByCustomerIdOrderByCreatedAtDesc(Long id);
  List<ShopOrder> findAllByOrderByCreatedAtDesc();
}
