package com.jotirling.repository;
import com.jotirling.entity.RouteAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RouteAnnouncementRepository extends JpaRepository<RouteAnnouncement,Long> {
 boolean existsByRouteIdAndTripDate(Long id,java.time.LocalDate date);
}
