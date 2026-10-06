package com.jotirling.repository;
import com.jotirling.entity.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderPaymentRepository extends JpaRepository<OrderPayment,Long> {
 List<OrderPayment> findByShopOrderIdOrderByIdAsc(Long id);
 Optional<OrderPayment> findByRequestId(String requestId);
 boolean existsByReferenceKey(String key);
}
