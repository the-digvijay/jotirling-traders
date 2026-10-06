package com.jotirling.controller;

import com.jotirling.dto.Inputs.*;
import com.jotirling.entity.Account;
import com.jotirling.service.*;
import com.jotirling.service.ShopService.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class Api {

  private final AuthService auth;
  private final ShopService shop;
  private final PushGateway push;
  private final boolean secure;

  public Api(
    AuthService a,
    ShopService s,
    PushGateway p,
    @Value("${app.cookie-secure}") boolean secure
  ) {
    auth = a;
    shop = s;
    push = p;
    this.secure = secure;
  }

  private void cookie(HttpServletResponse response, String value, long age) {
    response.addHeader(
      "Set-Cookie",
      ResponseCookie.from("jt_session", value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Strict")
        .path("/")
        .maxAge(age)
        .build()
        .toString()
    );
  }

  @GetMapping("/config")
  public Map<String, Object> config() {
    return Map.of(
      "categories",
      ShopService.CATEGORIES,
      "push",
      push.config(),
      "shopName",
      "ज्योतिर्लिंग ट्रेडर्स",
      "phone",
      "8381017384",
      "address",
      "गारगोटी, कोल्हापूर (महाराष्ट्र)"
    );
  }

  @GetMapping("/products")
  public List<ProductView> catalog() {
    return shop.products(false);
  }

  @PostMapping("/auth/register")
  public AuthService.UserView register(
    @Valid @RequestBody Register in,
    HttpServletResponse response
  ) {
    Account a = auth.register(in);
    cookie(response, auth.createSession(a), 604800);
    return auth.view(a);
  }

  @PostMapping("/auth/login")
  public AuthService.UserView login(
    @Valid @RequestBody Login in,
    HttpServletResponse response
  ) {
    Account a = auth.login(in);
    cookie(response, auth.createSession(a), 604800);
    return auth.view(a);
  }

  @GetMapping("/auth/me")
  public AuthService.UserView me(HttpServletRequest req) {
    return auth.view(auth.current(req));
  }

  @PostMapping("/auth/logout")
  public void logout(HttpServletRequest req, HttpServletResponse response) {
    auth.logout(req);
    cookie(response, "", 0);
  }

  @GetMapping("/orders")
  public List<OrderView> orders(HttpServletRequest req) {
    return shop.orders(auth.current(req));
  }

  @GetMapping("/orders/{id}")
  public OrderView order(@PathVariable Long id, HttpServletRequest req) {
    return shop.order(auth.current(req), id);
  }

  @PostMapping("/orders")
  public OrderView place(
    @Valid @RequestBody OrderInput in,
    HttpServletRequest req
  ) {
    return shop.place(auth.current(req), in);
  }

  @PostMapping("/orders/{id}/cancel")
  public OrderView cancel(@PathVariable Long id, HttpServletRequest req) {
    return shop.cancel(auth.current(req), id);
  }

  @GetMapping("/notifications")
  public List<NoticeView> inbox(HttpServletRequest req) {
    return shop.inbox(auth.current(req));
  }

  @PatchMapping("/notifications/{id}/read")
  public void read(@PathVariable Long id, HttpServletRequest req) {
    shop.read(auth.current(req), id);
  }

  @PostMapping("/push/subscribe")
  public void subscribe(
    @Valid @RequestBody SubscriptionInput in,
    HttpServletRequest req
  ) {
    push.subscribe(auth.current(req), in);
  }

  @PostMapping("/push/unsubscribe")
  public void unsubscribe(
    @RequestBody Map<String, String> in,
    HttpServletRequest req
  ) {
    push.unsubscribe(auth.current(req), in.getOrDefault("endpoint", ""));
  }

  @GetMapping("/admin/products")
  public List<ProductView> allProducts(HttpServletRequest req) {
    auth.admin(req);
    return shop.products(true);
  }

  @PostMapping("/admin/products")
  public ProductView add(
    @Valid @RequestBody ProductInput in,
    HttpServletRequest req
  ) {
    auth.admin(req);
    return shop.saveProduct(null, in);
  }

  @PutMapping("/admin/products/{id}")
  public ProductView edit(
    @PathVariable Long id,
    @Valid @RequestBody ProductInput in,
    HttpServletRequest req
  ) {
    auth.admin(req);
    return shop.saveProduct(id, in);
  }

  @PatchMapping("/admin/orders/{id}")
  public OrderView status(
    @PathVariable Long id,
    @Valid @RequestBody StatusInput in,
    HttpServletRequest req
  ) {
    auth.admin(req);
    return shop.status(id, in);
  }

  @GetMapping("/admin/purchases")
  public List<PurchaseView> purchases(HttpServletRequest req) {
    auth.admin(req);
    return shop.purchases();
  }

  @PostMapping("/admin/purchases")
  public PurchaseView purchase(
    @Valid @RequestBody PurchaseInput in,
    HttpServletRequest req
  ) {
    auth.admin(req);
    return shop.purchase(in);
  }

  @GetMapping("/admin/dashboard")
  public Map<String, Object> dashboard(HttpServletRequest req) {
    auth.admin(req);
    return shop.dashboard();
  }
}
