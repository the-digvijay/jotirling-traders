package com.jotirling.service;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
@Component public class SimpleOrderMigration implements ApplicationRunner {
 private final EntityManager em;
 public SimpleOrderMigration(EntityManager e){em=e;}
 @Override @Transactional public void run(ApplicationArguments args){
  em.createQuery("update ShopOrder o set o.status = 'CONFIRMED' where o.status in ('PREPARING','READY','DISPATCHED')").executeUpdate();
 }
}
