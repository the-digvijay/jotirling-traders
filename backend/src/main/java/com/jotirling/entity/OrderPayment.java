package com.jotirling.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="jt_orderpayment")
public class OrderPayment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public ShopOrder shopOrder;
 @ManyToOne(optional=false) public Account recordedBy;
 @Column(nullable=false,precision=15,scale=2) public BigDecimal amount;
 @Column(nullable=false,length=12) public String method;
 @Column(nullable=false,length=100) public String reference;
 @Column(nullable=false,length=500) public String notes;
 @Column(nullable=false) public LocalDate receivedDate;
 @Column(nullable=false,unique=true,length=36) public String requestId;
 @Column(nullable=false,length=64) public String requestHash;
 @Column(unique=true,length=120) public String referenceKey;
 public boolean voided=false;
 @Column(length=500) public String voidReason;
 public Instant voidedAt;
 @ManyToOne public Account voidedBy;
 public Instant createdAt=Instant.now();
}
