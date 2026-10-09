package com.jotirling.service;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import com.jotirling.dto.PaymentInputs.Receipt;
import jakarta.validation.constraints.*;
import java.time.*;
import java.math.*;
import java.util.*;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service @Transactional public class PaymentClaimService {
 private final PaymentClaimRepository claims;private final ShopOrderRepository orders;private final OrderPaymentRepository receipts;private final PaymentService payments;private final AccountRepository accounts;private final NoticeRepository notices;private final jakarta.persistence.EntityManager em;
 public PaymentClaimService(PaymentClaimRepository c,ShopOrderRepository o,OrderPaymentRepository r,PaymentService p,AccountRepository a,NoticeRepository n,jakarta.persistence.EntityManager e){claims=c;orders=o;receipts=r;payments=p;accounts=a;notices=n;em=e;}
 public record Input(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9-]{6,100}") String reference,@NotNull @PastOrPresent LocalDate paidDate,@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String requestId){}
 public record Review(@NotNull Boolean approve,@NotNull @Size(max=500) String notes,boolean bankChecked){}
 public record View(Long id,Long orderId,String customerName,String customerPhone,BigDecimal amount,String reference,LocalDate paidDate,String status,String reviewNotes,String reviewedBy,Instant reviewedAt,Long receiptId,Instant createdAt,String requestId){}
 private ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
 private PaymentClaim required(Long id){return claims.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment submission not found"));}
 private ShopOrder lock(Long id){return orders.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));}
 private void allowed(Account a,ShopOrder o){if(!"ADMIN".equals(a.role)&&!o.customer.id.equals(a.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your order");}
 private View view(PaymentClaim c){return new View(c.id,c.shopOrder.id,c.shopOrder.customer.name,c.shopOrder.customer.phone,c.amount,c.reference,c.paidDate,c.status,c.reviewNotes,c.reviewedBy==null?null:c.reviewedBy.name,c.reviewedAt,c.receiptId,c.createdAt,c.requestId);}
 private void notify(Account a,Long order,String title,String body){Notice n=new Notice();n.account=a;n.orderId=order;n.title=title;n.body=body;notices.save(n);}
 public List<View> list(Account a,Long orderId){ShopOrder o=orders.findById(orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));allowed(a,o);return claims.findByShopOrderIdOrderByIdDesc(orderId).stream().map(this::view).toList();}
 public List<View> all(Account admin){if(!"ADMIN".equals(admin.role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin only");return claims.findAllByOrderByIdDesc().stream().map(this::view).toList();}
 public PaymentClaim image(Account a,Long id){PaymentClaim c=required(id);allowed(a,c.shopOrder);return c;}
 public View submit(Account customer,Long orderId,Input in,byte[] bytes,String mediaType){
 if(!"CUSTOMER".equals(customer.role))throw bad("Use your customer account to submit payment");
 ShopOrder o=lock(orderId);allowed(customer,o);try{if(!UUID.fromString(in.requestId()).toString().equalsIgnoreCase(in.requestId()))throw bad("Invalid request ID");}catch(IllegalArgumentException e){throw bad("Invalid request ID");}
 BigDecimal amount=in.amount().setScale(2,RoundingMode.UNNECESSARY);String ref=in.reference().trim().toUpperCase(Locale.ROOT);
 String signature;try{signature=AuthService.hash(orderId+"|"+amount+"|"+ref+"|"+in.paidDate()+"|"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));}catch(Exception e){throw new IllegalStateException(e);}
 PaymentClaim old=claims.findByRequestId(in.requestId()).orElse(null);if(old!=null){if(!old.shopOrder.id.equals(o.id)||!old.requestHash.equals(signature))throw new ResponseStatusException(HttpStatus.CONFLICT,"Request already used with different details");return view(old);}
 if(!"DELIVERED".equals(o.status))throw bad("Payment submission is available after delivery");
 if(amount.compareTo(payments.summary(o).balance())>0)throw bad("Amount exceeds the remaining balance");
 String key="UPI:"+ref;if(claims.existsByReferenceKey(key)||receipts.existsByReferenceKey(key))throw new ResponseStatusException(HttpStatus.CONFLICT,"This UTR is already submitted or recorded");
 PaymentClaim c=new PaymentClaim();c.shopOrder=o;c.amount=amount;c.reference=ref;c.referenceKey=key;c.requestId=in.requestId();c.requestHash=signature;c.paidDate=in.paidDate();c.screenshot=bytes;c.mediaType=mediaType;claims.saveAndFlush(c);
 for(Account admin:accounts.findByRole("ADMIN"))notify(admin,o.id,"Payment verification pending #"+o.id,customer.name+" submitted INR "+amount+". Check your bank/UPI credit before verifying.");return view(c);
 }
 public View review(Account admin,Long claimId,Review in){
 if(!"ADMIN".equals(admin.role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin only");PaymentClaim initial=required(claimId);ShopOrder o=lock(initial.shopOrder.id);em.refresh(o,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);em.refresh(initial);PaymentClaim c=initial;
 if(!"PENDING".equals(c.status)){if((in.approve()&&"VERIFIED".equals(c.status))||(!in.approve()&&"REJECTED".equals(c.status)))return view(c);throw new ResponseStatusException(HttpStatus.CONFLICT,"This submission has already been reviewed");}
 if(in.approve()){
 if(!in.bankChecked())throw bad("Confirm the matching credit in your bank/UPI statement before verifying");
 if(!"DELIVERED".equals(o.status))throw bad("Order must be delivered before verification");
 var summary=payments.record(admin,o.id,new Receipt(c.amount,"UPI",c.reference,"Verified customer submission #"+c.id,c.paidDate,c.requestId));
 c.receiptId=summary.history().stream().filter(p->p.requestId().equals(c.requestId)).findFirst().orElseThrow().id();c.status="VERIFIED";
 }else{if(in.notes().isBlank())throw bad("Enter a rejection reason");c.status="REJECTED";c.referenceKey=null;notify(o.customer,o.id,"Payment submission rejected #"+o.id,in.notes().trim());}
 c.reviewNotes=in.notes().trim();c.reviewedBy=admin;c.reviewedAt=Instant.now();claims.saveAndFlush(c);return view(c);
 }
}
