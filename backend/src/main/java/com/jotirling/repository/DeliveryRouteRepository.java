package com.jotirling.repository;
import com.jotirling.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from DeliveryRoute r where r.id = :id")
 Optional<DeliveryRoute> lockById(@Param("id") Long id);
}
