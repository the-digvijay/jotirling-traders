package com.jotirling.repository;
import com.jotirling.entity.AccountPayment;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountPaymentRepository extends JpaRepository<AccountPayment,Long>{
 java.util.Optional<AccountPayment> findByRequestId(String id);
 boolean existsByProofKey(String key);
 java.util.List<AccountPayment> findByCustomerIdOrderByIdDesc(Long id);
 java.util.List<AccountPayment> findAllByOrderByIdDesc();
}
