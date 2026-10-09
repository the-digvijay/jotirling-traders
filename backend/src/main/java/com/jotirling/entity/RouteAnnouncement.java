package com.jotirling.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="jt_routeannouncement",uniqueConstraints=@UniqueConstraint(columnNames={"route_id","tripDate"}))
public class RouteAnnouncement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public DeliveryRoute route;
 @Column(nullable=false) public LocalDate tripDate;
}
