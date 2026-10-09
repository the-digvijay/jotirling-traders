package com.jotirling.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="jt_openingbalance") public class OpeningBalance {
 @Id public Long id;
 @Column(nullable=false,precision=15,scale=2) public BigDecimal amount;
 @ManyToOne public Account changedBy;
 public Instant changedAt;
}
