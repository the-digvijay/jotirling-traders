package com.jotirling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jotirling.dto.Inputs.SubscriptionInput;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import java.net.URI;
import java.security.Security;
import java.time.*;
import java.util.*;
import nl.martijndwars.webpush.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PushGateway {

  private final PushSubscriptionRepository subscriptions;
  private final NoticeRepository notices;
  private final ObjectMapper mapper;
  private final String publicKey, privateKey, subject;

  public PushGateway(
    PushSubscriptionRepository s,
    NoticeRepository n,
    ObjectMapper m,
    @Value("${app.push-public}") String pub,
    @Value("${app.push-private}") String priv,
    @Value("${app.push-subject}") String subj
  ) {
    subscriptions = s;
    notices = n;
    mapper = m;
    publicKey = pub;
    privateKey = priv;
    subject = subj;
    Security.addProvider(new BouncyCastleProvider());
  }

  public Map<String, Object> config() {
    return Map.of(
      "enabled",
      !publicKey.isBlank() && !privateKey.isBlank(),
      "publicKey",
      publicKey
    );
  }

  @Transactional
  public void subscribe(Account account, SubscriptionInput in) {
    try {
      URI uri = URI.create(in.endpoint());
      String host = uri.getHost();
      boolean allowed =
        host != null &&
        (host.equals("fcm.googleapis.com") ||
          host.endsWith(".push.services.mozilla.com") ||
          host.equals("web.push.apple.com") ||
          host.endsWith(".notify.windows.com"));
      if (
        !uri.getScheme().equals("https") ||
        !allowed ||
        (uri.getPort() != -1 && uri.getPort() != 443) ||
        uri.getUserInfo() != null ||
        Base64.getUrlDecoder().decode(in.p256dh()).length != 65 ||
        Base64.getUrlDecoder().decode(in.auth()).length != 16
      ) throw new IllegalArgumentException();
    } catch (Exception e) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Unsupported push subscription"
      );
    }
    String hash = AuthService.hash(in.endpoint());
    PushSubscription s = subscriptions
      .findByEndpointHash(hash)
      .orElse(new PushSubscription());
    s.account = account;
    s.endpointHash = hash;
    s.endpoint = in.endpoint();
    s.p256dh = in.p256dh();
    s.auth = in.auth();
    subscriptions.save(s);
  }

  @Transactional
  public void unsubscribe(Account a, String endpoint) {
    subscriptions
      .findByEndpointHash(AuthService.hash(endpoint))
      .ifPresent(s -> {
        if (s.account.id.equals(a.id)) subscriptions.delete(s);
      });
  }

  @Scheduled(fixedDelay = 15000)
  @Transactional
  public void dispatch() {
    if (publicKey.isBlank() || privateKey.isBlank()) return;
    for (Notice n : notices.findTop30ByPushDoneFalseAndPushAttemptsLessThanAndNextPushAtBeforeOrderByIdAsc(
      5,
      Instant.now()
    )) {
      boolean failed = false;
      var devices = subscriptions.findByAccountId(n.account.id);
      for (var device : devices) {
        try {
          var service = new PushService(publicKey, privateKey, subject);
          String payload = mapper.writeValueAsString(
            Map.of(
              "title",
              n.title,
              "body",
              n.body,
              "url",
              "/?order=" + (n.orderId == null ? "" : n.orderId),
              "tag",
              "notice-" + n.id
            )
          );
          var cfg = org.apache.http.client.config.RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build();
          try (
            var client = org.apache.http.impl.client.HttpClients.custom()
              .setDefaultRequestConfig(cfg)
              .disableRedirectHandling()
              .build();
            var response = client.execute(
              service.preparePost(
                new Notification(
                  device.endpoint,
                  device.p256dh,
                  device.auth,
                  payload
                ),
                Encoding.AES128GCM
              )
            )
          ) {
            int code = response.getStatusLine().getStatusCode();
            if (code == 404 || code == 410) subscriptions.delete(device);
            else if (code < 200 || code >= 300) failed = true;
          }
        } catch (Exception e) {
          failed = true;
        }
      }
      n.pushAttempts++;
      n.pushDone = !failed;
      n.nextPushAt = Instant.now().plusSeconds(60L * n.pushAttempts);
      notices.save(n);
    }
  }
}
