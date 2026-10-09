package com.jotirling.service;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional
public class RouteService {
 private final DeliveryRouteRepository routes; private final CustomerRouteRepository links;
 private final RouteAnnouncementRepository announcements; private final AccountRepository accounts; private final NoticeRepository notices;
 public RouteService(DeliveryRouteRepository r,CustomerRouteRepository l,RouteAnnouncementRepository a,AccountRepository c,NoticeRepository n){routes=r;links=l;announcements=a;accounts=c;notices=n;}
 public record Input(@NotBlank @Size(max=100) String name,@NotNull @Size(max=300) String villages,@Min(0) @Max(7) int day,boolean active,@NotNull @Pattern(regexp="([01][0-9]|2[0-3]):[0-5][0-9]") String cutoff){}
 public record Assignment(Long routeId){}
 public record View(Long id,String name,String villages,int day,boolean active,String cutoff,LocalDate nextVisit){}
 private DeliveryRoute required(Long id){return routes.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Route not found"));}
 private LocalDate next(DeliveryRoute r){if(!r.active||r.day<2||r.day>7)return null;LocalDate d=LocalDate.now(ZoneId.of("Asia/Kolkata"));return d.plusDays((r.day-d.getDayOfWeek().getValue()+7)%7);}
 private View view(DeliveryRoute r){return new View(r.id,r.name,r.villages,r.day,r.active,r.cutoff,next(r));}
 public List<View> list(boolean admin){return routes.findAll().stream().filter(r->admin||r.active).map(this::view).toList();}
 public View save(Long id,Input in){if(in.day()==1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Monday is a holiday. Choose Tuesday to Sunday or Unscheduled.");DeliveryRoute r=id==null?new DeliveryRoute():required(id);r.name=in.name().trim();r.villages=in.villages().trim();r.day=in.day();r.active=in.active();r.cutoff=in.cutoff();return view(routes.save(r));}
 public void defaults(){if(routes.count()>0)return;for(String n:List.of("कोल्हापूर","सिंधुदुर्ग","कर्नाटक","गडहिंग्लज","कागल","कराड","कोकण","आजरा")){DeliveryRoute r=new DeliveryRoute();r.name=n;routes.save(r);}}
 public void assign(Long accountId,Long routeId){Account a=accounts.findById(accountId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found"));if(!"CUSTOMER".equals(a.role))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select a customer");if(routeId==null){links.deleteById(accountId);return;}DeliveryRoute r=required(routeId);if(!r.active)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Route is inactive");CustomerRoute l=new CustomerRoute();l.id=accountId;l.route=r;links.save(l);}
 public Map<String,Object> mine(Account a){CustomerRoute l=links.findById(a.id).orElse(null);Map<String,Object> m=new LinkedHashMap<>();m.put("route",l==null?null:view(l.route));return m;}
 public List<Map<String,Object>> customers(){return accounts.findByRole("CUSTOMER").stream().map(a->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.id);m.put("name",a.name);m.put("phone",a.phone);m.put("routeId",links.findById(a.id).map(l->l.route.id).orElse(null));return m;}).toList();}
 public int announce(Long id){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Route-wide announcements are disabled. Save delivery route/date on the accepted order; its customer receives a reminder the previous day.");}
 public void daily(){}
}
