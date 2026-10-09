package com.jotirling.entity;
import jakarta.persistence.*;
@Entity @Table(name="jt_customerroute")
public class CustomerRoute {
 @Id public Long id;
 @ManyToOne(optional=false) public DeliveryRoute route;
}
