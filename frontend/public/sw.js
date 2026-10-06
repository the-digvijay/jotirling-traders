self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (e) => e.waitUntil(self.clients.claim()));
// No authenticated API responses are cached. Ordering requires a connection.
self.addEventListener("fetch", (e) => {
  if (e.request.mode === "navigate")
    e.respondWith(
      fetch(e.request).catch(
        () =>
          new Response(
            "<h2>Jotirling Traders</h2><p>You are offline. Reconnect to view orders and stock.</p>",
            { headers: { "Content-Type": "text/html" } },
          ),
      ),
    );
});
self.addEventListener("push", (e) => {
  let data = {
    title: "Jotirling Traders",
    body: "You have an order update.",
    url: "/",
  };
  try {
    data = { ...data, ...e.data.json() };
  } catch {}
  e.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      icon: "/icon-192.png",
      badge: "/icon-192.png",
      tag: data.tag || "jt-update",
      data: { url: data.url },
    }),
  );
});
self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  e.waitUntil(
    (async () => {
      const target = new URL(
        e.notification.data?.url || "/",
        self.location.origin,
      );
      if (target.origin !== self.location.origin) return;
      const clients = await self.clients.matchAll({
        type: "window",
        includeUncontrolled: true,
      });
      for (const c of clients) {
        await c.navigate(target.href);
        return c.focus();
      }
      return self.clients.openWindow(target.href);
    })(),
  );
});
