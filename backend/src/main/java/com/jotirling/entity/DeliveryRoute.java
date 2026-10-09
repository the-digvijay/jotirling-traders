package com.jotirling.entity;
import jakarta.persistence.*;
@Entity @Table(name="jt_deliveryroute")
public class DeliveryRoute {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=100) public String name;
 @Column(nullable=false,length=300) public String villages="";
 @Column(name="weekday_number") public int day=0;
 public boolean active=true;
 @Column(nullable=false,length=5) public String cutoff="11:00";
}
