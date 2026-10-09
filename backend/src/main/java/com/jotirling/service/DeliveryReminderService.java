package com.jotirling.service;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional public class DeliveryReminderService {
 private final ShopOrderRepository orders; private final DeliveryRouteRepository routes; private final NoticeRepository notices;
 public DeliveryReminderService(ShopOrderRepository o,DeliveryRouteRepository r,NoticeRepository n){orders=o;routes=r;notices=n;}
 public record Plan(@NotNull Long routeId,@NotNull LocalDate date){}
 private ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
 public void plan(Long id,Plan in){ShopOrder o=orders.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));if(!"DELIVERY".equals(o.fulfilment)||!Set.of("PENDING","CONFIRMED","PREPARING","READY").contains(o.status))throw bad("Only pending or accepted undelivered delivery orders can be scheduled");LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));if(in.date().isBefore(today))throw bad("Choose today or a future date");if(in.date().getDayOfWeek()==DayOfWeek.MONDAY)throw bad("Monday is a holiday");DeliveryRoute r=routes.findById(in.routeId()).orElseThrow(()->bad("Route not found"));if(!r.active)throw bad("Route is inactive");boolean changed=o.deliveryRoute==null||!o.deliveryRoute.id.equals(r.id)||!in.date().equals(o.deliveryDate);if(changed){o.reminderSentFor=null;o.deliveryRoute=r;o.deliveryDate=in.date();orders.save(o);if(!"PENDING".equals(o.status)){Notice n=new Notice();n.account=o.customer;n.orderId=o.id;n.title="तुमच्या delivery फेरीचे वेळापत्रक बदलले";n.body="Order #"+o.id+" • Route: "+r.name+" • गाडी येण्याची तारीख: "+in.date();notices.save(n);}}}
 public int remind(){LocalDate tomorrow=LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);int count=0;for(ShopOrder candidate:orders.findByDeliveryDate(tomorrow)){ShopOrder o=orders.lockById(candidate.id).orElseThrow();if(!tomorrow.equals(o.deliveryDate)||!"DELIVERY".equals(o.fulfilment)||o.deliveryRoute==null||!o.deliveryRoute.active||!Set.of("CONFIRMED","PREPARING","READY","DISPATCHED").contains(o.status)||tomorrow.equals(o.reminderSentFor))continue;Notice n=new Notice();n.account=o.customer;n.orderId=o.id;n.title="उद्या तुमच्या route वर गाडी येणार आहे";n.body="उद्या Jotirling Traders ची गाडी तुमच्या "+o.deliveryRoute.name+" route वर येणार आहे. तुमची स्वीकारलेली Order #"+o.id+" या फेरीसाठी नियोजित आहे. Delivery तारीख: "+tomorrow;notices.save(n);o.reminderSentFor=tomorrow;orders.save(o);count++;}return count;}
}
