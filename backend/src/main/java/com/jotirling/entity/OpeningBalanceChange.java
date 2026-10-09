package com.jotirling.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="jt_openingbalancechange") public class OpeningBalanceChange {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Account customer;
 @ManyToOne(optional=false) public Account changedBy;
 public BigDecimal oldAmount;
 public BigDecimal newAmount;
 public Instant changedAt=Instant.now();
}
