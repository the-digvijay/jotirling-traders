package com.jotirling.service;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import com.jotirling.dto.Inputs.StatusInput;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional public class OrderConfirmationService {
 private final ShopOrderRepository orders;private final CustomerRouteRepository links;private final DeliveryRouteRepository routes;private final DeliveryReminderService plans;private final ShopService shop;private final EntityManager em;
 public OrderConfirmationService(ShopOrderRepository o,CustomerRouteRepository l,DeliveryRouteRepository r,DeliveryReminderService p,ShopService s,EntityManager e){orders=o;links=l;routes=r;plans=p;shop=s;em=e;}
 public record Input(Long routeId,LocalDate date,@NotNull @Size(max=500) String notes){}
 public record Suggestion(Long routeId,String routeName,LocalDate date,String source,String message){}
 private void admin(Account a){if(!"ADMIN".equals(a.role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin only");}
 private ShopOrder required(Long id){return orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));}
 private LocalDate next(DeliveryRoute r){if(!r.active||r.day<2||r.day>7)return null;ZonedDateTime now=ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));LocalDate today=now.toLocalDate();int days=(r.day-today.getDayOfWeek().getValue()+7)%7;if(days==0&&!now.toLocalTime().isBefore(LocalTime.parse(r.cutoff)))days=7;return today.plusDays(days);}
 @Transactional(readOnly=true) public Suggestion suggest(Account a,Long id,Long selected){admin(a);ShopOrder o=required(id);if(!"DELIVERY".equals(o.fulfilment))return new Suggestion(null,null,null,"pickup","Pickup orderला delivery route लागत नाही.");DeliveryRoute r=null;String source="customer";LocalDate date=null;if(selected!=null){r=routes.findById(selected).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Route not found"));source="selected";}else if(o.deliveryRoute!=null&&o.deliveryRoute.active){r=o.deliveryRoute;source="saved";if(o.deliveryDate!=null&&!o.deliveryDate.isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata"))))date=o.deliveryDate;}else{CustomerRoute link=links.findById(o.customer.id).orElse(null);if(link!=null&&link.route.active)r=link.route;}
 if(r==null||!r.active)return new Suggestion(null,null,null,source,"दुकानदाराचा route सेट नाही किंवा बंद आहे. Route निवडा किंवा Routesमध्ये एकदा जोडा.");if(date==null)date=next(r);return new Suggestion(r.id,r.name,date,source,date==null?"या routeचा आठवड्याचा दिवस सेट नाही. तारीख निवडा.":"फेरी आपोआप सुचवली आहे. गरज असल्यास route किंवा तारीख बदला.");}
 public ShopService.OrderView confirm(Account a,Long id,Input in){admin(a);ShopOrder o=orders.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));em.refresh(o,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
 if("CONFIRMED".equals(o.status)){if("DELIVERY".equals(o.fulfilment)&&((in.routeId()!=null&&!Objects.equals(in.routeId(),o.deliveryRoute==null?null:o.deliveryRoute.id))||(in.date()!=null&&!Objects.equals(in.date(),o.deliveryDate))))throw new ResponseStatusException(HttpStatus.CONFLICT,"Order is already confirmed. Edit its delivery plan separately.");return shop.order(a,id);}
 if(!"PENDING".equals(o.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Only a pending order can be confirmed");
 if("DELIVERY".equals(o.fulfilment)){Suggestion s=suggest(a,id,in.routeId());Long route=in.routeId()==null?s.routeId():in.routeId();LocalDate date=in.date()==null?s.date():in.date();if(route==null||date==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a delivery route and date");plans.plan(id,new DeliveryReminderService.Plan(route,date));orders.flush();}
 return shop.status(id,new StatusInput("CONFIRMED",in.notes()));
 }
}
