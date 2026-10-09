package com.jotirling.repository;
import com.jotirling.entity.PaymentClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentClaimRepository extends JpaRepository<PaymentClaim,Long>{
 Optional<PaymentClaim> findByRequestId(String id);
 boolean existsByReferenceKey(String key);
 List<PaymentClaim> findByShopOrderIdOrderByIdDesc(Long id);
 List<PaymentClaim> findAllByOrderByIdDesc();
}
