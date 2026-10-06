package com.jotirling.service;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import com.jotirling.dto.PaymentInputs.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;
@Service @Transactional
public class PaymentService {
 private final OrderPaymentRepository payments;
 private final NoticeRepository notices;
 private final EntityManager em;
 public PaymentService(OrderPaymentRepository p,NoticeRepository n,EntityManager e){payments=p;notices=n;em=e;}
 public record PaymentView(Long id,BigDecimal amount,String method,String reference,String notes,LocalDate receivedDate,Instant createdAt,String recordedBy,boolean voided,String voidReason,Instant voidedAt,String voidedBy,String requestId) {}
 public record Summary(BigDecimal paid,BigDecimal balance,String status,List<PaymentView> history) {}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 public static boolean chargeable(String status){return Set.of("CONFIRMED","PREPARING","READY","DISPATCHED","DELIVERED").contains(status);}
 private PaymentView view(OrderPayment p){return new PaymentView(p.id,p.amount,p.method,p.reference,p.notes,p.receivedDate,p.createdAt,p.recordedBy.name,p.voided,p.voidReason,p.voidedAt,p.voidedBy==null?null:p.voidedBy.name,p.requestId);}
 @Transactional(readOnly=true) public Summary summary(ShopOrder order){var rows=payments.findByShopOrderIdOrderByIdAsc(order.id);BigDecimal paid=rows.stream().filter(p->!p.voided).map(p->p.amount).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2);BigDecimal balance=chargeable(order.status)?order.total.subtract(paid).setScale(2):BigDecimal.ZERO.setScale(2);String status=!chargeable(order.status)?"NOT_DUE":balance.signum()==0?"PAID":paid.signum()>0?"PARTIAL":"UNPAID";return new Summary(paid,balance,status,rows.stream().map(this::view).toList());}
 private ShopOrder lock(Long id){ShopOrder o=em.find(ShopOrder.class,id,LockModeType.PESSIMISTIC_WRITE);if(o==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found");em.refresh(o,LockModeType.PESSIMISTIC_WRITE);return o;}
 private void notify(Account account,Long id,String title,String body){Notice n=new Notice();n.account=account;n.orderId=id;n.title=title;n.body=body;notices.save(n);}
 public Summary record(Account admin,Long orderId,Receipt in){
 if(!admin.role.equals("ADMIN"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin access required");
 if(!UUID.fromString(in.requestId()).toString().equalsIgnoreCase(in.requestId()))throw bad("Invalid request ID");
 ShopOrder o=lock(orderId);BigDecimal amount=in.amount().setScale(2,RoundingMode.UNNECESSARY);
 String reference=in.reference().trim().toUpperCase(Locale.ROOT);
 String signature;try{signature=AuthService.hash(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(List.of(orderId,amount,in.method(),reference,in.notes().trim(),in.receivedDate().toString())));}catch(Exception e){throw new IllegalStateException(e);}
 var old=payments.findByRequestId(in.requestId()).orElse(null);
 if(old!=null){if(!old.shopOrder.id.equals(orderId)||!old.requestHash.equals(signature))throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment request ID already used with different details");return summary(o);}
 if(!chargeable(o.status))throw bad("Confirm the order before recording a payment. Cancelled/rejected orders cannot accept payments");
 if(amount.compareTo(summary(o).balance())>0)throw bad("Payment exceeds the outstanding balance");
 if(!in.method().equals("CASH")&&reference.isBlank())throw bad("Enter the UPI transaction reference / bank UTR");
 OrderPayment p=new OrderPayment();p.shopOrder=o;p.recordedBy=admin;p.amount=amount;p.method=in.method();p.reference=reference;p.notes=in.notes().trim();p.receivedDate=in.receivedDate();p.requestId=in.requestId();p.requestHash=signature;
 if(!in.method().equals("CASH")){p.referenceKey=in.method()+":"+reference;if(payments.existsByReferenceKey(p.referenceKey))throw new ResponseStatusException(HttpStatus.CONFLICT,"This transaction reference is already recorded");}
 payments.saveAndFlush(p);notify(o.customer,o.id,"Payment recorded #"+o.id,"Shop recorded INR "+amount.toPlainString()+" by "+in.method()+". Open the app to see your balance.");return summary(o);
 }
 public Summary voidEntry(Account admin,Long orderId,Long paymentId,VoidInput in){
 if(!admin.role.equals("ADMIN"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin access required");ShopOrder o=lock(orderId);OrderPayment p=payments.findById(paymentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found"));
 if(!p.shopOrder.id.equals(orderId))throw bad("Payment does not belong to this order");if(p.voided)return summary(o);
 p.voided=true;p.voidReason=in.reason().trim();p.voidedAt=Instant.now();p.voidedBy=admin;p.referenceKey=null;payments.saveAndFlush(p);
 notify(o.customer,o.id,"Payment entry corrected #"+o.id,"Shop voided an incorrect payment entry of INR "+p.amount.toPlainString()+". Check the revised balance in the app.");return summary(o);
 }
}
