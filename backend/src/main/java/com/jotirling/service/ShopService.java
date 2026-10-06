package com.jotirling.service;

import com.jotirling.dto.Inputs.*;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ShopService {

  public static final List<String> CATEGORIES = List.of(
    "WELDING",
    "WELDING_ACCESSORIES",
    "PAINTS",
    "PLUMBING",
    "ELECTRICAL",
    "HARDWARE",
    "PLYWOOD",
    "CEMENT",
    "SANITARY",
    "TOOLS"
  );
  private final ProductRepository products;
  private final ShopOrderRepository orders;
  private final OrderLineRepository lines;
  private final PurchaseRepository purchases;
  private final NoticeRepository notices;
  private final AccountRepository accounts;
  private final EntityManager em;
  private final PaymentService payments;

  public ShopService(
    ProductRepository p,
    ShopOrderRepository o,
    OrderLineRepository l,
    PurchaseRepository x,
    NoticeRepository n,
    AccountRepository a,
    EntityManager e,
    PaymentService paymentService
  ) {
    products = p;
    orders = o;
    lines = l;
    purchases = x;
    notices = n;
    accounts = a;
    em = e;
    payments = paymentService;
  }

  public record ProductView(
    Long id,
    String sku,
    String name,
    String category,
    String variant,
    String unit,
    String description,
    BigDecimal price,
    int stock,
    int reserved,
    int available,
    int lowStock,
    boolean active,
    String imageUrl
  ) {}

  public record LineView(
    Long id,
    Long productId,
    String name,
    String variant,
    String unit,
    int quantity,
    BigDecimal price,
    BigDecimal amount
  ) {}

  public record OrderView(
    Long id,
    Long customerId,
    String customerName,
    String customerPhone,
    String status,
    String fulfilment,
    String address,
    String customerNotes,
    String shopNotes,
    BigDecimal total,
    Instant createdAt,
    Instant updatedAt,
    List<LineView> items,
    PaymentService.Summary payment
  ) {}

  public record PurchaseView(
    Long id,
    Long productId,
    String productName,
    String supplier,
    String invoiceReference,
    int quantity,
    BigDecimal unitCost,
    BigDecimal total,
    LocalDate receivedDate
  ) {}

  public record NoticeView(
    Long id,
    String title,
    String body,
    Long orderId,
    boolean read,
    Instant createdAt
  ) {}

  private String fingerprint(Object value) {
    try {
      return AuthService.hash(
        new com.fasterxml.jackson.databind.ObjectMapper()
          .writeValueAsString(value)
      );
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ResponseStatusException bad(String msg) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
  }

  private <T> T require(Optional<T> v) {
    return v.orElseThrow(() ->
      new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found")
    );
  }

  private ProductView view(Product p) {
    return new ProductView(
      p.id,
      p.sku,
      p.name,
      p.category,
      p.variant,
      p.unit,
      p.description,
      p.price,
      p.stock,
      p.reserved,
      p.stock - p.reserved,
      p.lowStock,
      p.active,
      p.imageVersion == null ? null : "/products/" + p.id + "/image?v=" + p.imageVersion
    );
  }

  private OrderView view(ShopOrder o) {
    return new OrderView(
      o.id,
      o.customer.id,
      o.customer.name,
      o.customer.phone,
      o.status,
      o.fulfilment,
      o.address,
      o.customerNotes,
      o.shopNotes,
      o.total,
      o.createdAt,
      o.updatedAt,
      lines
        .findByShopOrderIdOrderByProductIdAsc(o.id)
        .stream()
        .map(l ->
          new LineView(
            l.id,
            l.product.id,
            l.name,
            l.variant,
            l.unit,
            l.quantity,
            l.price,
            l.amount
          )
        )
        .toList(),
      payments.summary(o)
    );
  }

  private PurchaseView view(Purchase p) {
    return new PurchaseView(
      p.id,
      p.product.id,
      p.productName,
      p.supplier,
      p.invoiceReference,
      p.quantity,
      p.unitCost,
      p.total,
      p.receivedDate
    );
  }

  @Transactional(readOnly = true)
  public List<ProductView> products(boolean admin) {
    return products
      .findAllByOrderByIdAsc()
      .stream()
      .filter(p -> admin || p.active)
      .map(this::view)
      .toList();
  }

  public ProductView saveProduct(Long id, ProductInput in) {
    if (!CATEGORIES.contains(in.category())) throw bad("Invalid category");
    Product p = id == null ? new Product() : lockProduct(id);
    p.sku = in.sku().trim().toUpperCase(Locale.ROOT);
    p.name = in.name().trim();
    p.category = in.category();
    p.variant = in.variant().trim();
    p.unit = in.unit().trim();
    p.description = in.description().trim();
    p.price = in.price().setScale(2);
    p.lowStock = in.lowStock();
    p.active = in.active();
    return view(products.saveAndFlush(p));
  }

  private Product lockProduct(Long id) {
    Product p = em.find(Product.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (p == null) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Product not found"
    );
    // Order lines may already have loaded this product before the lock.
    // Refresh under the lock so concurrent confirmations see committed stock.
    em.refresh(p, LockModeType.PESSIMISTIC_WRITE);
    return p;
  }

  private ShopOrder lockOrder(Long id) {
    ShopOrder o = em.find(ShopOrder.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (o == null) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Order not found"
    );
    return o;
  }

  private void uuid(String request) {
    try {
      if (
        !UUID.fromString(request).toString().equalsIgnoreCase(request)
      ) throw new IllegalArgumentException();
    } catch (Exception e) {
      throw bad("Invalid request ID");
    }
  }

  private void notify(Account a, String title, String body, Long id) {
    Notice n = new Notice();
    n.account = a;
    n.title = title;
    n.body = body;
    n.orderId = id;
    notices.save(n);
  }

  public OrderView place(Account customer, OrderInput in) {
    if (!customer.role.equals("CUSTOMER")) throw bad(
      "Use a customer account to place orders"
    );
    uuid(in.requestId());
    if (in.fulfilment().equals("DELIVERY") && in.address().isBlank()) throw bad(
      "Enter delivery address"
    );
    em.find(Account.class, customer.id, LockModeType.PESSIMISTIC_WRITE);
    var quantities = new TreeMap<Long, Integer>();
    for (var l : in.items()) {
      if (quantities.put(l.productId(), l.quantity()) != null) throw bad(
        "Duplicate product in cart"
      );
    }
    String signature = fingerprint(
      List.of(
        in.fulfilment(),
        in.address().trim(),
        in.notes().trim(),
        quantities
      )
    );
    var existing = orders.findByRequestId(in.requestId()).orElse(null);
    if (existing != null) {
      if (
        !existing.customer.id.equals(customer.id) ||
        !existing.requestHash.equals(signature)
      ) throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Request ID already used"
      );
      return view(existing);
    }
    ShopOrder o = new ShopOrder();
    o.customer = customer;
    o.requestId = in.requestId();
    o.requestHash = signature;
    o.fulfilment = in.fulfilment();
    o.address = in.address().trim();
    o.customerNotes = in.notes().trim();
    o.total = BigDecimal.ZERO;
    var rows = new ArrayList<OrderLine>();
    for (var e : quantities.entrySet()) {
      Product p = lockProduct(e.getKey());
      if (!p.active || p.stock - p.reserved < e.getValue()) throw bad(
        "Insufficient available stock: " + p.name
      );
      OrderLine l = new OrderLine();
      l.product = p;
      l.name = p.name;
      l.variant = p.variant;
      l.unit = p.unit;
      l.quantity = e.getValue();
      l.price = p.price;
      l.amount = p.price.multiply(BigDecimal.valueOf(l.quantity)).setScale(2);
      o.total = o.total.add(l.amount);
      rows.add(l);
    }
    if (o.total.compareTo(new BigDecimal("9999999999999.99")) > 0) throw bad(
      "Order total exceeds supported limit"
    );
    orders.save(o);
    for (var l : rows) {
      l.shopOrder = o;
      lines.save(l);
    }
    for (var a : accounts.findByRole("ADMIN")) notify(
      a,
      "New order #" + o.id,
      customer.name + " submitted a new order. Review and confirm it.",
      o.id
    );
    notify(
      customer,
      "Order received #" + o.id,
      "Your order is pending shop confirmation.",
      o.id
    );
    return view(o);
  }

  public OrderView status(Long id, StatusInput in) {
    ShopOrder o = lockOrder(id);
    String next = in.status();
    if (o.status.equals(next)) return view(o);
    boolean allowed =
      switch (o.status) {
        case "PENDING" -> Set.of("CONFIRMED", "REJECTED", "CANCELLED").contains(
          next
        );
        case "CONFIRMED" -> Set.of("PREPARING", "READY", "CANCELLED").contains(
          next
        );
        case "PREPARING" -> Set.of("READY", "CANCELLED").contains(next);
        case "READY" -> Set.of(
          o.fulfilment.equals("PICKUP") ? "DELIVERED" : "DISPATCHED",
          "CANCELLED"
        ).contains(next);
        case "DISPATCHED" -> next.equals("DELIVERED");
        default -> false;
      };
    if (!allowed) throw bad(
      "Invalid status transition: " + o.status + " → " + next
    );
    if (next.equals("CANCELLED") && payments.summary(o).paid().signum() > 0) throw bad("A paid order cannot be cancelled. Refund handling is not supported; contact the shop.");
    var rows = lines.findByShopOrderIdOrderByProductIdAsc(o.id);
    boolean hadReservation = Set.of(
      "CONFIRMED",
      "PREPARING",
      "READY",
      "DISPATCHED"
    ).contains(o.status);
    for (var l : rows) {
      Product p = lockProduct(l.product.id);
      if (next.equals("CONFIRMED")) {
        if (!p.active || p.stock - p.reserved < l.quantity) throw bad(
          "Cannot confirm: insufficient stock for " + l.name
        );
        p.reserved += l.quantity;
      } else if (next.equals("DELIVERED")) {
        if (
          p.reserved < l.quantity || p.stock < l.quantity
        ) throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Inventory mismatch. Contact admin"
        );
        p.stock -= l.quantity;
        p.reserved -= l.quantity;
      } else if (next.equals("CANCELLED") && hadReservation) {
        if (p.reserved < l.quantity) throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Inventory mismatch"
        );
        p.reserved -= l.quantity;
      }
      products.save(p);
    }
    o.status = next;
    o.shopNotes = in.notes().trim();
    o.updatedAt = Instant.now();
    orders.save(o);
    notify(
      o.customer,
      "Order #" + o.id + " — " + next,
      "Your order status has changed. Open the app for details.",
      o.id
    );
    return view(o);
  }

  public OrderView cancel(Account customer, Long id) {
    ShopOrder o = lockOrder(id);
    if (!o.customer.id.equals(customer.id)) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Not your order"
    );
    if (o.status.equals("CANCELLED")) return view(o);
    if (!o.status.equals("PENDING")) throw bad(
      "Contact the shop to cancel a confirmed order"
    );
    o.status = "CANCELLED";
    o.updatedAt = Instant.now();
    orders.save(o);
    for (var a : accounts.findByRole("ADMIN")) notify(
      a,
      "Order cancelled #" + id,
      "The customer cancelled a pending order.",
      id
    );
    notify(
      customer,
      "Order cancelled #" + id,
      "Your pending order was cancelled.",
      id
    );
    return view(o);
  }

  @Transactional(readOnly = true)
  public List<OrderView> orders(Account a) {
    return (
      a.role.equals("ADMIN")
        ? orders.findAllByOrderByCreatedAtDesc()
        : orders.findByCustomerIdOrderByCreatedAtDesc(a.id)
    ).stream()
      .map(this::view)
      .toList();
  }

  @Transactional(readOnly = true)
  public OrderView order(Account a, Long id) {
    ShopOrder o = require(orders.findById(id));
    if (
      !a.role.equals("ADMIN") && !o.customer.id.equals(a.id)
    ) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
    return view(o);
  }

  public PurchaseView purchase(PurchaseInput in) {
    uuid(in.requestId());
    Product p = lockProduct(in.productId());
    String signature = fingerprint(
      List.of(
        in.productId(),
        in.supplier().trim(),
        in.invoiceReference().trim(),
        in.quantity(),
        in.unitCost().setScale(2),
        in.receivedDate().toString()
      )
    );
    var old = purchases.findByRequestId(in.requestId()).orElse(null);
    if (old != null) {
      if (!old.requestHash.equals(signature)) throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Request ID already used"
      );
      return view(old);
    }
    if ((long) p.stock + in.quantity() > 10000000) throw bad(
      "Stock limit exceeded"
    );
    Purchase x = new Purchase();
    x.product = p;
    x.productName = p.name + " " + p.variant;
    x.supplier = in.supplier().trim();
    x.invoiceReference = in.invoiceReference().trim();
    x.quantity = in.quantity();
    x.unitCost = in.unitCost().setScale(2);
    x.total = x.unitCost.multiply(BigDecimal.valueOf(x.quantity));
    if (x.total.compareTo(new BigDecimal("9999999999999.99")) > 0) throw bad(
      "Purchase total exceeds supported limit"
    );
    x.receivedDate = in.receivedDate();
    x.requestId = in.requestId();
    x.requestHash = signature;
    p.stock += x.quantity;
    products.save(p);
    return view(purchases.saveAndFlush(x));
  }

  @Transactional(readOnly = true)
  public List<PurchaseView> purchases() {
    return purchases
      .findAllByOrderByCreatedAtDesc()
      .stream()
      .map(this::view)
      .toList();
  }

  @Transactional(readOnly = true)
  public List<NoticeView> inbox(Account a) {
    return notices
      .findTop100ByAccountIdOrderByCreatedAtDescIdDesc(a.id)
      .stream()
      .map(n ->
        new NoticeView(
          n.id,
          n.title,
          n.body,
          n.orderId,
          n.readFlag,
          n.createdAt
        )
      )
      .toList();
  }

  public void read(Account a, Long id) {
    Notice n = require(notices.findById(id));
    if (!n.account.id.equals(a.id)) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Not your notification"
    );
    n.readFlag = true;
    notices.save(n);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> dashboard() {
    var all = orders.findAll();
    var p = products.findAll();
    return Map.of(
      "pending",
      all.stream().filter(o -> o.status.equals("PENDING")).count(),
      "active",
      all
        .stream()
        .filter(o ->
          Set.of("CONFIRMED", "PREPARING", "READY", "DISPATCHED").contains(
            o.status
          )
        )
        .count(),
      "delivered",
      all.stream().filter(o -> o.status.equals("DELIVERED")).count(),
      "sales",
      all
        .stream()
        .filter(o -> o.status.equals("DELIVERED"))
        .map(o -> o.total)
        .reduce(BigDecimal.ZERO, BigDecimal::add),
      "purchaseValue",
      purchases
        .findAll()
        .stream()
        .map(x -> x.total)
        .reduce(BigDecimal.ZERO, BigDecimal::add),
      "lowStock",
      p
        .stream()
        .filter(x -> x.active && x.stock - x.reserved <= x.lowStock)
        .map(this::view)
        .toList()
    );
  }
}
