package com.jotirling.service;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import com.jotirling.service.ShopService.LineView;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional(readOnly=true) public class TripManifestService {
 private final DeliveryRouteRepository routes;private final ShopOrderRepository orders;private final OrderLineRepository lines;
 public TripManifestService(DeliveryRouteRepository r,ShopOrderRepository o,OrderLineRepository l){routes=r;orders=o;lines=l;}
 public record Key(Long productId,String name,String variant,String unit){}
 public record LoadItem(Long productId,String name,String variant,String unit,long quantity){}
 public record DeliveryOrder(Long id,Long customerId,String customerName,String phone,String address,String customerNotes,String shopNotes,BigDecimal total,List<LineView> items){}
 public record Manifest(Long routeId,String routeName,String villages,LocalDate date,Instant generatedAt,long customers,int orderCount,BigDecimal orderValue,List<LoadItem> load,List<DeliveryOrder> orders){}
 public Manifest get(Long routeId,LocalDate date){DeliveryRoute r=routes.findById(routeId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Route not found"));List<ShopOrder> selected=orders.findByDeliveryDate(date).stream().filter(o->"CONFIRMED".equals(o.status)&&"DELIVERY".equals(o.fulfilment)&&o.deliveryRoute!=null&&routeId.equals(o.deliveryRoute.id)).sorted(Comparator.comparing((ShopOrder o)->o.customer.name).thenComparing(o->o.customer.id).thenComparing(o->o.id)).toList();Map<Key,Long> grouped=new LinkedHashMap<>();List<DeliveryOrder> out=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;Set<Long> customers=new HashSet<>();for(ShopOrder o:selected){List<LineView> items=new ArrayList<>();for(OrderLine l:lines.findByShopOrderIdOrderByProductIdAsc(o.id)){Key key=new Key(l.product.id,l.name,l.variant,l.unit);grouped.merge(key,(long)l.quantity,Long::sum);items.add(new LineView(l.id,l.product.id,l.name,l.variant,l.unit,l.quantity,l.price,l.amount));}out.add(new DeliveryOrder(o.id,o.customer.id,o.customer.name,o.customer.phone,o.address,o.customerNotes,o.shopNotes,o.total,items));customers.add(o.customer.id);total=total.add(o.total);}List<LoadItem> load=grouped.entrySet().stream().map(e->new LoadItem(e.getKey().productId(),e.getKey().name(),e.getKey().variant(),e.getKey().unit(),e.getValue())).sorted(Comparator.comparing(LoadItem::name).thenComparing(LoadItem::variant).thenComparing(LoadItem::unit)).toList();return new Manifest(r.id,r.name,r.villages,date,Instant.now(),customers.size(),out.size(),total,load,out);}
}
