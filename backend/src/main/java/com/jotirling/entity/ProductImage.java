package com.jotirling.entity;
import jakarta.persistence.*;
@Entity @Table(name="jt_productimage")
public class ProductImage {
 @Id public Long id;
 @Lob @Column(nullable=false, columnDefinition="MEDIUMBLOB") public byte[] data;
 @Column(nullable=false) public String mediaType;
}
