package com.jotirling.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="jt_accountpayment") public class AccountPayment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Account customer;
 @Column(nullable=false,precision=15,scale=2) public BigDecimal amount;
 @Column(nullable=false) public LocalDate paidDate;
 @Column(nullable=false,length=20) public String status="PENDING";
 @Column(nullable=false,unique=true,length=36) public String requestId;
 @Column(nullable=false,length=64) public String signature;
 @Column(unique=true,length=100) public String proofKey;
 @Lob @Column(nullable=false,columnDefinition="MEDIUMBLOB") public byte[] proof;
 @Column(nullable=false,length=30) public String mediaType;
 @Column(nullable=false,length=500) public String notes="";
 @ManyToOne public Account reviewedBy;
 public Instant reviewedAt;
 public Instant createdAt=Instant.now();
}
