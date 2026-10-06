import React, { useState, useEffect, useRef } from "react";
import { api, money, today, errorText } from "./api";
import OrderPayments, {PaymentLedger} from "./OrderPayments";
import {openBill} from "./documents";
const names = {
  WELDING: "वेल्डिंग साहित्य",
  WELDING_ACCESSORIES: "वेल्डिंग accessories",
  PAINTS: "पेंट्स",
  PLUMBING: "प्लंबिंग",
  ELECTRICAL: "इलेक्ट्रिकल",
  HARDWARE: "हार्डवेअर",
  PLYWOOD: "प्लायवुड",
  CEMENT: "सिमेंट",
  SANITARY: "सॅनिटरी",
  TOOLS: "टूल्स",
};
const symbols = {
  WELDING: "⚡",
  WELDING_ACCESSORIES: "🧤",
  PAINTS: "🎨",
  PLUMBING: "🚰",
  ELECTRICAL: "🔌",
  HARDWARE: "🔩",
  PLYWOOD: "▤",
  CEMENT: "▣",
  SANITARY: "◉",
  TOOLS: "🛠",
};
const blankProduct = () => ({
  sku: "",
  name: "",
  category: "WELDING",
  variant: "",
  unit: "piece",
  description: "",
  price: "",
  lowStock: 5,
  active: true,
});
function Field({ label, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}
function Status({ value }) {
  return (
    <span className={"status " + value.toLowerCase()}>
      {value.replaceAll("_", " ")}
    </span>
  );
}
const date = (x) =>
  new Date(x).toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
const productPhotos = {
  "WELD-001": "anu-bond.png", "WELD-002": "anu-bond.png",
  "ACC-001": "welding-gloves.jpg", "ACC-002": "welding-helmet.jpg",
  "PAINT-001": "asian-white.jpg", "PAINT-002": "asian-black.jpg",
  "PAINT-003": "asian-primer.jpg",
};
function ProductPhoto({ product, compact = false }) {
  const file = productPhotos[product.sku] ||
    (product.category === "WELDING" && /anu\s*bond/i.test(product.name) ? "anu-bond.png" : null);
  const source = product.imageUrl ? `${api.defaults.baseURL.replace(/\/$/, "")}${product.imageUrl}` : file ? `/products/${file}` : null;
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [source]);
  if (!source || failed) return <><span>{symbols[product.category]}</span><small>{names[product.category]}</small></>;
  return <>
    <img src={source} alt={`${product.name} — product reference photo`}
      loading="lazy" onError={() => setFailed(true)}
      style={{ width: "100%", height: compact ? "65px" : "165px", objectFit: "contain", display: "block", background: "white" }} />
    {!compact && !product.imageUrl && <small style={{ color: "#554b3d", padding: "0 8px", textAlign: "center" }}>
      {file === "anu-bond.png" ? "Anu Bond · pack size variantनुसार तपासा" : "प्रातिनिधिक फोटो · brand, model व pack प्रत्यक्ष मालानुसार तपासा"}
    </small>}
  </>;
}
function SelectedPhotoPreview({ file }) {
  const [url, setUrl] = useState("");
  useEffect(() => {
    if (!file) { setUrl(""); return; }
    const value = URL.createObjectURL(file); setUrl(value);
    return () => URL.revokeObjectURL(value);
  }, [file]);
  return url ? <img src={url} alt="Selected product photo" style={{width:140,height:140,objectFit:"contain",display:"block",margin:"12px 0"}} /> : null;
}

export default function App() {
  const [user, setUser] = useState(null),
    [authReady, setAuthReady] = useState(false),
    [config, setConfig] = useState({
      categories: Object.keys(names),
      push: { enabled: false },
    }),
    [page, setPage] = useState("Catalogue"),
    [products, setProducts] = useState([]),
    [orders, setOrders] = useState([]),
    [inbox, setInbox] = useState([]),
    [purchases, setPurchases] = useState([]),
    [dashboard, setDashboard] = useState({}),
    [search, setSearch] = useState(""),
    [category, setCategory] = useState(""),
    [statusFilter, setStatusFilter] = useState(""),
    [orderSearch, setOrderSearch] = useState(""),
    [orderFromDate, setOrderFromDate] = useState(""),
    [orderToDate, setOrderToDate] = useState(""),
    [cart, setCart] = useState({}),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [authMode, setAuthMode] = useState(null),
    [credentials, setCredentials] = useState({
      name: "",
      email: "",
      phone: "",
      password: "",
    }),
    [product, setProduct] = useState(blankProduct),
    [editing, setEditing] = useState(null),
    [productPhoto, setProductPhoto] = useState(null),
    [photoInputKey, setPhotoInputKey] = useState(0),
    [checkout, setCheckout] = useState({
      fulfilment: "PICKUP",
      address: "",
      notes: "",
    }),
    [purchase, setPurchase] = useState({
      productId: "",
      supplier: "",
      invoiceReference: "",
      quantity: 1,
      unitCost: "",
      receivedDate: today(),
    }),
    [orderNotes, setOrderNotes] = useState({}),
    [selectedOrder, setSelectedOrder] = useState(null),
    [installPrompt, setInstallPrompt] = useState(null),
    [pushStatus, setPushStatus] = useState(""),
    [alerts, setAlerts] = useState(false),
    [sound, setSound] = useState(false);
  const guard = useRef(false),
    seen = useRef(new Set()),
    audio = useRef(null),
    pendingOrder = useRef(null),
    pendingPurchase = useRef(null),
    userRef = useRef(null),
    lastError = useRef(null);
  const admin = user?.role === "ADMIN";
  userRef.current = user;
  async function playLoudAlert() {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) throw new Error("This browser does not support alert sound.");
    if (!audio.current || audio.current.state === "closed") {
      audio.current = new AudioCtx();
    }
    const ctx = audio.current;
    await ctx.resume();
    if (ctx.state !== "running") throw new Error("Browser blocked alert sound.");
    for (let i = 0; i < 3; i++) {
      const start = ctx.currentTime + i * 0.85;
      const oscillator = ctx.createOscillator();
      const gain = ctx.createGain();
      oscillator.type = "square";
      oscillator.frequency.setValueAtTime(900, start);
      oscillator.frequency.setValueAtTime(1200, start + 0.25);
      gain.gain.setValueAtTime(0, start);
      gain.gain.linearRampToValueAtTime(0.3, start + 0.03);
      gain.gain.setValueAtTime(0.3, start + 0.55);
      gain.gain.linearRampToValueAtTime(0, start + 0.65);
      oscillator.connect(gain);
      gain.connect(ctx.destination);
      oscillator.start(start);
      oscillator.stop(start + 0.7);
      oscillator.onended = () => {
        oscillator.disconnect();
        gain.disconnect();
      };
    }
  }
  async function publicLoad() {
    const [p, c] = await Promise.all([
      api.get("/products"),
      api.get("/config"),
    ]);
    setProducts(p.data);
    setConfig(c.data);
  }
  async function privateLoad(u, notify = false) {
    if (!u) return;
    const calls = [api.get("/orders"), api.get("/notifications")];
    if (u.role === "ADMIN")
      calls.push(
        api.get("/admin/products"),
        api.get("/admin/purchases"),
        api.get("/admin/dashboard"),
      );
    else calls.push(api.get("/products"));
    const data = await Promise.all(calls);
    if (userRef.current?.id !== u.id) return;
    setOrders(data[0].data);
    setInbox(data[1].data);
    if (u.role === "ADMIN") {
      setProducts(data[2].data);
      setPurchases(data[3].data);
      setDashboard(data[4].data);
    } else setProducts(data[2].data);
    for (const n of data[1].data) {
      if (notify && !seen.current.has(n.id) && !n.read) {
        setNotice(n.title);
        if (sound) {
          try {
            await playLoudAlert();
          } catch (e) {
            console.error("Alert sound failed:", e);
          }
        }
        if (
          alerts &&
          "Notification" in window &&
          Notification.permission === "granted"
        ) {
          try {
            const reg = await navigator.serviceWorker.ready;
            await reg.showNotification(n.title, {
              body: n.body,
              icon: "/icon-192.png",
              tag: "notice-" + n.id,
              data: { url: "/?order=" + n.orderId },
            });
          } catch {}
        }
      }
      seen.current.add(n.id);
    }
  }
  async function refresh(u = user) {
    setLoading(true);
    try {
      if (u) await privateLoad(u);
      else await publicLoad();
      setError("");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    (async () => {
      try {
        await publicLoad();
        try {
          const r = await api.get("/auth/me");
          userRef.current = r.data;
          setUser(r.data);
          setPage(
            new URLSearchParams(location.search).has("order")
              ? "Orders"
              : r.data.role === "ADMIN"
                ? "Dashboard"
                : "Catalogue",
          );
          await privateLoad(r.data);
        } catch (e) {
          if (e.response?.status !== 401) throw e;
        }
      } catch (e) {
        setError(errorText(e));
      } finally {
        setAuthReady(true);
        setLoading(false);
      }
    })();
    const install = (e) => {
      e.preventDefault();
      setInstallPrompt(e);
    };
    window.addEventListener("beforeinstallprompt", install);
    return () => window.removeEventListener("beforeinstallprompt", install);
  }, []);
  useEffect(() => {
    if (!user) return;
    let running = false;
    const timer = setInterval(async () => {
      if (running || guard.current) return;
      running = true;
      try {
        await privateLoad(user, true);
      } catch (e) {
        if (e.response?.status === 401) {
          setError("Session expired. Please sign in again.");
          userRef.current = null;
          setUser(null);
          setOrders([]);
          setInbox([]);
          setPurchases([]);
          setDashboard({});
          setProducts([]);
          setCart({});
          setSelectedOrder(null);
          pendingOrder.current = null;
          pendingPurchase.current = null;
          setPage("Catalogue");
          publicLoad().catch(() => {});
        }
      } finally {
        running = false;
      }
    }, 10000);
    return () => clearInterval(timer);
  }, [user?.id, alerts, sound]);
  async function run(fn, message) {
    if (guard.current) return null;
    guard.current = true;
    lastError.current = null;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const r = await fn();
      if (message) setNotice(message);
      return r;
    } catch (e) {
      lastError.current = e;
      setError(errorText(e));
      return null;
    } finally {
      guard.current = false;
      setBusy(false);
    }
  }
  function orderDateKey(value) {
    const parsed = new Date(value);
    if (!value || Number.isNaN(parsed.getTime())) return "";
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone: "Asia/Kolkata", year: "numeric", month: "2-digit", day: "2-digit"
    }).formatToParts(parsed);
    const part = (type) => parts.find((p) => p.type === type)?.value;
    return `${part("year")}-${part("month")}-${part("day")}`;
  }

  function hasOutstandingPayment(order) {
    return ["CONFIRMED", "PREPARING", "READY", "DISPATCHED", "DELIVERED"].includes(order.status)
      && Number(order.payment?.balance || 0) > 0;
  }

  function matchesOrderFilter(order) {
    const matchesStatus = !statusFilter || (
      statusFilter === "OUTSTANDING"
        ? admin && hasOutstandingPayment(order)
        : statusFilter === "IN_PROGRESS"
        ? ["CONFIRMED", "PREPARING", "READY", "DISPATCHED"].includes(order.status)
        : order.status === statusFilter
    );
    const query = orderSearch.trim().toLowerCase();
    const matchesSearch = !query || (
      String(order.id).includes(query.replace(/^#/, "")) ||
      String(order.customerName || "").toLowerCase().includes(query) ||
      String(order.customerPhone || "").includes(query)
    );
    const day = orderDateKey(order.createdAt);
    const matchesDates = (!orderFromDate || (day && day >= orderFromDate)) &&
      (!orderToDate || (day && day <= orderToDate));
    return matchesStatus && matchesSearch && matchesDates;
  }

  function openDashboardCard(label) {
    setOrderSearch("");
    setOrderFromDate("");
    setOrderToDate("");
    if (label === "Today’s orders" || label === "Today’s order sales") {
      const day = orderDateKey(new Date().toISOString());
      nav("Orders");
      setOrderFromDate(day);
      setOrderToDate(day);
      setStatusFilter(label === "Today’s order sales" ? "DELIVERED" : "");
      return;
    }
    if (label === "Outstanding / बाकी" || label === "Orders with balance") {
      nav("Orders");
      setStatusFilter("OUTSTANDING");
      return;
    }
    if (label === "Purchase value") {
      nav("Purchases");
      return;
    }
    if (label === "Low-stock products") {
      document.getElementById("low-stock-alerts")?.scrollIntoView({
        behavior: "smooth", block: "start"
      });
      return;
    }
    const filters = {
      "Pending orders": "PENDING",
      "In progress": "IN_PROGRESS",
      "Delivered": "DELIVERED",
      "Delivered sales": "DELIVERED",
    };
    nav("Orders");
    setStatusFilter(filters[label] || "");
  }

  function nav(next) {
    setPage(next);
    setError("");
    setNotice("");
    setSelectedOrder(null);
  }
  async function authenticate(e) {
    e.preventDefault();
    const r = await run(() =>
      api.post(
        "/auth/" + (authMode === "register" ? "register" : "login"),
        credentials,
      ),
    );
    if (r) {
      userRef.current = r.data;
      setUser(r.data);
      seen.current = new Set();
      setAuthMode(null);
      setCredentials({ name: "", email: "", phone: "", password: "" });
      setPage(
        new URLSearchParams(location.search).has("order")
          ? "Orders"
          : r.data.role === "ADMIN"
            ? "Dashboard"
            : "Catalogue",
      );
      setCart({});
      await refresh(r.data);
    }
  }
  async function logout() {
    const r = await run(async () => {
      if ("serviceWorker" in navigator) {
        const reg = await navigator.serviceWorker.getRegistration();
        const s = await reg?.pushManager?.getSubscription();
        if (s) {
          await api.post("/push/unsubscribe", { endpoint: s.endpoint });
          await s.unsubscribe();
        }
      }
      await api.post("/auth/logout");
      return true;
    });
    if (r) {
      setUser(null);
      userRef.current = null;
      setCart({});
      setOrders([]);
      setInbox([]);
      setPurchases([]);
      setDashboard({});
      pendingOrder.current = null;
      pendingPurchase.current = null;
      seen.current = new Set();
      setAlerts(false);
      setSound(false);
      setSelectedOrder(null);
      setOrderNotes({});
      setPushStatus("");
      setPage("Catalogue");
      await refresh(null);
    }
  }
  async function enableNotifications() {
    await run(async () => {
      if (
        !("Notification" in window) ||
        !("serviceWorker" in navigator) ||
        !window.isSecureContext
      )
        throw new Error("Notification support requires localhost or HTTPS.");
      const permission = await Notification.requestPermission();
      if (permission !== "granted") {
        setPushStatus(
          "Notifications not allowed. You can still use the app inbox.",
        );
        return true;
      }
      setAlerts(true);
      if (!config.push.enabled) {
        setPushStatus(
          "Browser alerts enabled while app is open. Background push needs VAPID setup.",
        );
        return true;
      }
      const reg = await navigator.serviceWorker.ready;
      const encoded = config.push.publicKey
        .replace(/-/g, "+")
        .replace(/_/g, "/");
      const key = Uint8Array.from(atob(encoded), (c) => c.charCodeAt(0));
      const sub =
        (await reg.pushManager.getSubscription()) ||
        (await reg.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: key,
        }));
      const s = sub.toJSON();
      await api.post("/push/subscribe", {
        endpoint: s.endpoint,
        p256dh: s.keys.p256dh,
        auth: s.keys.auth,
      });
      setPushStatus(
        "Background push subscription saved. Delivery depends on browser/network settings.",
      );
      return true;
    });
  }
  function add(p) {
    if (pendingOrder.current) {
      setNotice("Retry your pending cart request before changing the cart.");
      setPage("Cart");
      return;
    }
    if (!user) {
      setAuthMode("login");
      setNotice("Sign in to add products and place orders.");
      return;
    }
    if (admin) {
      setNotice("Use a customer account to place an order.");
      return;
    }
    setCart((c) => ({
      ...c,
      [p.id]: Math.min((c[p.id] || 0) + 1, p.available),
    }));
  }
  const cartRows = Object.entries(cart)
    .map(([id, quantity]) => ({
      p: products.find((p) => p.id === Number(id)),
      quantity,
    }))
    .filter((x) => x.p);
  const cartTotal = cartRows.reduce(
    (sum, x) => sum + Number(x.p.price) * x.quantity,
    0,
  );
  async function place(e) {
    e.preventDefault();
    if (!cartRows.length) return;
    const payload = {
      ...checkout,
      items: cartRows.map((x) => ({
        productId: x.p.id,
        quantity: Number(x.quantity),
      })),
    };
    if (!pendingOrder.current)
      pendingOrder.current = { ...payload, requestId: crypto.randomUUID() };
    const r = await run(() => api.post("/orders", pendingOrder.current));
    if (r) {
      pendingOrder.current = null;
      setCart({});
      setCheckout({ fulfilment: "PICKUP", address: "", notes: "" });
      setPage("Orders");
      setNotice("Order sent. दुकानाच्या confirmation ची प्रतीक्षा करा.");
      await refresh();
    } else if (
      lastError.current?.response?.status >= 400 &&
      lastError.current?.response?.status < 500
    ) {
      pendingOrder.current = null;
      setNotice("Order was not accepted. Correct the details and try again.");
    } else
      setNotice(
        "Before creating another request, check Orders. Retry uses the same request ID.",
      );
  }
  async function saveProduct(e) {
    e.preventDefault();
    const r = await run(async () => {
      const saved = editing ? await api.put("/admin/products/" + editing, product) : await api.post("/admin/products", product);
      setEditing(saved.data.id);
      if (productPhoto) {
        const body = new FormData(); body.append("file", productPhoto);
        try { await api.post(`/admin/products/${saved.data.id}/image`, body); }
        catch (e) { throw new Error("Product save झाला, पण photo upload झाला नाही. पुन्हा Save product करा. " + errorText(e)); }
      }
      return saved;
    }, "Product आणि photo saved");
    if (r) {
      setProduct(blankProduct()); setEditing(null); setProductPhoto(null); setPhotoInputKey((k) => k + 1);
      await refresh();
    }
  }
  async function receivePurchase(e) {
    e.preventDefault();
    if (!pendingPurchase.current)
      pendingPurchase.current = {
        ...purchase,
        productId: Number(purchase.productId),
        quantity: Number(purchase.quantity),
        unitCost: Number(purchase.unitCost),
        requestId: crypto.randomUUID(),
      };
    const r = await run(
      () => api.post("/admin/purchases", pendingPurchase.current),
      "Purchase received and stock updated",
    );
    if (r) {
      pendingPurchase.current = null;
      setPurchase({
        productId: "",
        supplier: "",
        invoiceReference: "",
        quantity: 1,
        unitCost: "",
        receivedDate: today(),
      });
      await refresh();
    } else if (
      lastError.current?.response?.status >= 400 &&
      lastError.current?.response?.status < 500
    ) {
      pendingPurchase.current = null;
      setNotice(
        "Purchase was not accepted. Correct the details and try again.",
      );
    }
  }
  async function updateStatus(o, status) {
    if (!confirm(`Order #${o.id}: change to ${status}?`)) return;
    const r = await run(
      () =>
        api.patch("/admin/orders/" + o.id, {
          status,
          notes: orderNotes[o.id] || "",
        }),
      "Order updated; customer notification created",
    );
    if (r) {
      setSelectedOrder(r.data);
      await refresh();
    }
  }
  function whatsappLink(order) {
    const raw = String(admin ? order.customerPhone : "8381017384");
    let phone = raw.replace(/\D/g, "");
    if (phone.length === 10) phone = "91" + phone;
    if (!/^91[0-9]{10}$/.test(phone)) return null;
    const statusLabels = {
      PENDING: "दुकानाच्या confirmation ची प्रतीक्षा",
      CONFIRMED: "Order confirm / book झाली आहे",
      PREPARING: "Order तयार करत आहोत",
      READY: "Order तयार आहे",
      DISPATCHED: "Order पाठवली आहे",
      DELIVERED: "Order delivered झाली आहे",
      REJECTED: "Order स्वीकारली नाही",
      CANCELLED: "Order cancel झाली आहे",
    };
    const message = [
      "ज्योतिर्लिंग ट्रेडर्स, गारगोटी",
      admin ? `नमस्कार ${order.customerName}, तुमच्या orderचे update:` : "नमस्कार, माझ्या appमधील orderची माहिती:",
      `Order #${order.id}`,
      `Customer: ${order.customerName}`,
      `Status: ${statusLabels[order.status] || order.status}`,
      `Fulfilment: ${order.fulfilment === "PICKUP" ? "दुकानातून pickup" : "Delivery"}`,
      ...order.items.slice(0, 5).map((item) =>
        `${item.name.slice(0, 60)}${item.variant ? " (" + item.variant.slice(0, 30) + ")" : ""}: ${item.quantity} ${item.unit}`
      ),
      ...(order.items.length > 5 ? [`आणखी ${order.items.length - 5} productsची माहिती appमध्ये पाहा.`] : []),
      `Order total: ${money(order.total)}`,
      `Paid: ${money(order.payment?.paid)}`,
      `Balance: ${money(order.payment?.balance)}`,
      `Payment status: ${order.payment?.status || "NOT_DUE"}`,
      ...(admin && order.shopNotes ? [`Shop note: ${order.shopNotes.slice(0, 200)}`] : []),
      "पूर्ण माहिती appमधील Ordersमध्ये पाहा.",
    ].join("\n");
    return `https://wa.me/${phone}?text=${encodeURIComponent(message)}`;
  }
  function nextStatuses(o) {
    switch (o.status) {
      case "PENDING":
        return ["CONFIRMED", "REJECTED", "CANCELLED"];
      case "CONFIRMED":
        return ["PREPARING", "READY", "CANCELLED"];
      case "PREPARING":
        return ["READY", "CANCELLED"];
      case "READY":
        return [
          o.fulfilment === "PICKUP" ? "DELIVERED" : "DISPATCHED",
          "CANCELLED",
        ];
      case "DISPATCHED":
        return ["DELIVERED"];
      default:
        return [];
    }
  }
  const menu = admin
    ? ["Dashboard", "Products", "Orders", "Payments", "Purchases", "Notifications"]
    : ["Catalogue", "Cart", "Orders", "Payments", "Notifications"];
  const todayKey = orderDateKey(new Date().toISOString());
  const todayAdminOrders = admin
    ? orders.filter((o) => orderDateKey(o.createdAt) === todayKey)
    : [];
  const todayAdminSales = todayAdminOrders
    .filter((o) => o.status === "DELIVERED")
    .reduce((sum, o) => sum + Number(o.total || 0), 0);
  const outstandingAdminOrders = admin ? orders.filter(hasOutstandingPayment) : [];
  const outstandingAdminCents = outstandingAdminOrders.reduce(
    (sum, o) => sum + Math.round(Number(o.payment.balance) * 100), 0
  );
  const unread = inbox.filter((n) => !n.read).length;
  return (
    <div className="app">
      <header className="siteheader no-print">
        <button
          className="brand"
          onClick={() => nav(admin ? "Dashboard" : "Catalogue")}
        >
          <img className="monogram shop-brand-logo" src="/shop-logo.jpeg" alt="ज्योतिर्लिंग ट्रेडर्स" />
          <span>
            <strong>ज्योतिर्लिंग ट्रेडर्स</strong>
            <small>गारगोटी · गुणवत्ता • विश्वास • सेवा</small>
          </span>
        </button>
        <div className="headerbuttons">
          {installPrompt && (
            <button
              className="btn ghost"
              onClick={async () => {
                await installPrompt.prompt();
                setInstallPrompt(null);
              }}
            >
              Install app
            </button>
          )}
          {user ? (
            <>
              <span className="username">
                {user.name} · {admin ? "Shop admin" : "Customer"}
              </span>
              <button className="btn ghost" disabled={busy} onClick={logout}>
                Logout
              </button>
            </>
          ) : (
            <button
              className="btn gold"
              disabled={!authReady}
              onClick={() => setAuthMode("login")}
            >
              Sign in / नोंदणी
            </button>
          )}
        </div>
      </header>
      <div className="layout">
        <aside className="sidebar no-print">
          <p className="caption">
            {admin ? "SHOP WORKSPACE" : "CUSTOMER STORE"}
          </p>
          {menu.map((m) => (
            <button
              className={page === m ? "nav selected" : "nav"}
              key={m}
              onClick={() => {
                if (!user && ["Orders", "Payments", "Notifications", "Cart"].includes(m)) {
                  setAuthMode("login");
                  return;
                }
                nav(m);
              }}
            >
              {m}
              {m === "Cart" && cartRows.length > 0 && <b>{cartRows.length}</b>}
              {m === "Notifications" && unread > 0 && <b>{unread}</b>}
            </button>
          ))}
          <div className="shopcontact">
            <strong>Jotirling Traders</strong>
            <p>गारगोटी, कोल्हापूर</p>
            <a href="tel:8381017384">☎ 8381017384</a>
            <small>Orders subject to shop confirmation.</small>
          </div>
        </aside>
        <main>
          <div className="titlebar no-print">
            <div>
              <span className="caption">JOTIRLING TRADERS · GARGOTI</span>
              <h1>
                {page === "Catalogue" ? "आपल्या कामासाठी योग्य साहित्य" : page}
              </h1>
            </div>
            <button
              className="btn outline"
              disabled={busy || loading}
              onClick={() => refresh()}
            >
              ↻ Refresh
            </button>
          </div>
          {error && (
            <div className="alert error" role="alert">
              {error}
            </div>
          )}
          {notice && (
            <div className="alert success" role="status">
              {notice}
            </div>
          )}
          {loading && <p className="muted no-print">Loading…</p>}
          {page === "Catalogue" && (
            <>
              <section className="hero">
                <div>
                  <span className="pill">
                    YOUR LOCAL BUILDING & WELDING STORE
                  </span>
                  <h2>
                    आपल्या विश्वासाचे नाते…
                    <br />
                    आमची गुणवत्ता!
                  </h2>
                  <p>
                    वेल्डिंग, पेंट्स, हार्डवेअर आणि बांधकाम साहित्य.
                    <br />
                    Products निवडा आणि तुमची order थेट दुकानाला पाठवा.
                  </p>
                  <div className="actions">
                    <a className="btn gold" href="#catalog">
                      Products पाहा ↓
                    </a>
                    <a
                      className="btn light"
                      href="/shop-logo.jpeg"
                      target="_blank"
                      rel="noreferrer"
                    >
                      दुकानाचा logo ↗
                    </a>
                  </div>
                </div>
                <img
                  className="shop-hero-logo"
                  src="/shop-logo.jpeg"
                  alt="ज्योतिर्लिंग ट्रेडर्स दुकानाचा लोगो"
                />
              </section>
              <section className="benefits">
                <span>✓ दुकानातून order confirmation</span>
                <span>✓ Pickup किंवा delivery request</span>
                <span>✓ Order status updates</span>
              </section>
              <section id="catalog">
                <div className="filters">
                  <input
                    aria-label="Search products"
                    placeholder="Product, brand, SKU किंवा variant शोधा…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                  />
                  <select
                    aria-label="Category"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                  >
                    <option value="">सर्व categories</option>
                    {config.categories.map((k) => (
                      <option key={k} value={k}>
                        {names[k]}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="categories">
                  {config.categories.map((k) => (
                    <button
                      key={k}
                      className={
                        category === k ? "category active" : "category"
                      }
                      onClick={() => setCategory(category === k ? "" : k)}
                    >
                      <span>{symbols[k]}</span>
                      {names[k]}
                    </button>
                  ))}
                </div>
                <div className="productgrid">
                  {products
                    .filter(
                      (p) =>
                        p.active &&
                        (!category || p.category === category) &&
                        (p.name + " " + p.variant + " " + p.sku)
                          .toLowerCase()
                          .includes(search.toLowerCase()),
                    )
                    .map((p) => (
                      <article className="productcard" key={p.id}>
                        <div className={"productart " + p.category.toLowerCase()}
                          style={{ height: "205px", background: "white" }}>
                          <ProductPhoto key={p.sku} product={p} />
                        </div>
                        <div className="productbody">
                          <small>{p.sku}</small>
                          <h3>{p.name}</h3>
                          <p>{p.variant || p.unit}</p>
                          <div className="price">
                            {money(p.price)}
                            <small> / {p.unit}</small>
                          </div>
                          <div className="stock">
                            {p.available > 0
                              ? `${p.available} ${p.unit} available`
                              : "Out of stock"}
                          </div>
                          <button
                            className="btn gold full"
                            disabled={busy || p.available < 1}
                            onClick={() => add(p)}
                          >
                            Add to cart +
                          </button>
                        </div>
                      </article>
                    ))}
                </div>
                {!products.filter(
                  (p) =>
                    p.active &&
                    (!category || p.category === category) &&
                    (p.name + " " + p.variant + " " + p.sku)
                      .toLowerCase()
                      .includes(search.toLowerCase()),
                ).length && (
                  <div className="empty panel">
                    No products found. Admin can add actual products and receive
                    stock in Purchases.
                  </div>
                )}
              </section>
            </>
          )}
          {page === "Cart" && (
            <section className="panel">
              <h2>तुमची order</h2>
              {cartRows.length ? (
                <>
                  <div className="tablewrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Product</th>
                          <th>Quantity</th>
                          <th>Amount</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {cartRows.map(({ p, quantity }) => (
                          <tr key={p.id}>
                            <td>
                              <strong>{p.name}</strong>
                              <small>
                                {p.variant} · {money(p.price)} / {p.unit}
                              </small>
                            </td>
                            <td>
                              <input
                                className="quantity"
                                type="number"
                                min="1"
                                max={p.available}
                                value={quantity}
                                disabled={busy || !!pendingOrder.current}
                                onChange={(e) =>
                                  setCart({
                                    ...cart,
                                    [p.id]: Math.max(
                                      1,
                                      Math.min(
                                        p.available,
                                        Number(e.target.value) || 1,
                                      ),
                                    ),
                                  })
                                }
                              />
                            </td>
                            <td>{money(Number(p.price) * quantity)}</td>
                            <td>
                              <button
                                className="textbtn"
                                disabled={busy || !!pendingOrder.current}
                                onClick={() => {
                                  const c = { ...cart };
                                  delete c[p.id];
                                  setCart(c);
                                }}
                              >
                                Remove
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <div className="total">
                    Estimated total: {money(cartTotal)}
                  </div>
                  <form onSubmit={place}>
                    <fieldset disabled={busy || !!pendingOrder.current}>
                      <div className="formrow">
                        <Field label="Fulfilment">
                          <select
                            value={checkout.fulfilment}
                            onChange={(e) =>
                              setCheckout({
                                ...checkout,
                                fulfilment: e.target.value,
                              })
                            }
                          >
                            <option value="PICKUP">दुकानातून pickup</option>
                            <option value="DELIVERY">Delivery request</option>
                          </select>
                        </Field>
                        <Field label="Address / delivery location">
                          <textarea
                            rows="2"
                            required={checkout.fulfilment === "DELIVERY"}
                            maxLength={500}
                            value={checkout.address}
                            onChange={(e) =>
                              setCheckout({
                                ...checkout,
                                address: e.target.value,
                              })
                            }
                          />
                        </Field>
                      </div>
                      <Field label="Order notes — optional">
                        <textarea
                          rows="2"
                          maxLength={500}
                          value={checkout.notes}
                          onChange={(e) =>
                            setCheckout({ ...checkout, notes: e.target.value })
                          }
                        />
                      </Field>
                    </fieldset>
                    <p className="muted">
                      दुकान stock आणि delivery तपासून order confirm करेल. Online
                      payment घेतले जात नाही. Delivery charges असल्यास दुकान
                      वेगळे कळवेल.
                    </p>
                    <button className="btn gold" disabled={busy}>
                      {pendingOrder.current
                        ? "Retry same order"
                        : "Send order to shop →"}
                    </button>
                  </form>
                </>
              ) : (
                <div className="empty">
                  Cart रिकामी आहे.
                  <button className="btn gold" onClick={() => nav("Catalogue")}>
                    Products पाहा
                  </button>
                </div>
              )}
            </section>
          )}
          {page === "Orders" && (
            <>
            <p className="muted no-print">
              WhatsApp button दाबल्यावर तयार message उघडेल. स्वतः तपासून Send दाबा.
            </p>
              <div className="filters no-print">
                <input
                  type="search"
                  aria-label="Search orders by number, customer name or mobile"
                  placeholder="Order # / Customer name / Mobile"
                  value={orderSearch}
                  onChange={(e) => setOrderSearch(e.target.value)}
                  style={{ flex: "1 1 240px", minWidth: 0 }}
                />
                {orderSearch && (
                  <button className="btn" onClick={() => setOrderSearch("")}>
                    Clear search
                  </button>
                )}
                <label>
                  From Date
                  <input type="date" value={orderFromDate}
                    max={orderToDate || undefined}
                    onChange={(e) => setOrderFromDate(e.target.value)} />
                </label>
                <label>
                  To Date
                  <input type="date" value={orderToDate}
                    min={orderFromDate || undefined}
                    onChange={(e) => setOrderToDate(e.target.value)} />
                </label>
                {(orderFromDate || orderToDate) && (
                  <button className="btn" onClick={() => {
                    setOrderFromDate(""); setOrderToDate("");
                  }}>Clear dates</button>
                )}
                <select
                  aria-label="Order status"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                >
                  <option value="">All statuses</option>
                  <option value="IN_PROGRESS">In progress</option>
                  {admin && <option value="OUTSTANDING">Outstanding / बाकी पैसे</option>}
                  {[
                    "PENDING",
                    "CONFIRMED",
                    "PREPARING",
                    "READY",
                    "DISPATCHED",
                    "DELIVERED",
                    "REJECTED",
                    "CANCELLED",
                  ].map((s) => (
                    <option key={s}>{s}</option>
                  ))}
                </select>
                <span className="muted">
                  Orders आणि notifications दर 10 seconds ला refresh होतात.
                </span>
              </div>
              {admin && statusFilter === "OUTSTANDING" && (
                <section className="panel">
                  <h3>बाकी पैसे असलेल्या orders</h3>
                  <p>सध्याच्या filtersनुसार {orders.filter(matchesOrderFilter).length} orders · बाकी रक्कम: <strong>{money(orders.filter(matchesOrderFilter).reduce((sum, o) => sum + Math.round(Number(o.payment.balance) * 100), 0) / 100)}</strong></p>
                  <p className="muted">खाली customerचे नाव, mobile आणि प्रत्येक orderची बाकी रक्कम दिसेल. पैसे मिळाल्यावर Paymentमध्ये नोंद करा. Pending, Cancelled आणि Rejected orders धरलेल्या नाहीत.</p>
                </section>
              )}
              {orderFromDate && orderToDate && orderFromDate > orderToDate && (
                <p role="alert">From Date ही To Dateच्या आधीची किंवा त्याच दिवशीची असावी.</p>
              )}
              <p className="muted no-print">Dates order तयार झालेल्या दिवसानुसार आहेत (भारत वेळ). {orders.filter(matchesOrderFilter).length} orders दिसत आहेत.</p>
              {orders
                .filter(matchesOrderFilter)
                .map((o) => (
                  <section className="panel order" key={o.id}>
                    <div className="sectiontitle">
                      <div>
                        <h3>Order #{o.id}</h3>
                        <small>
                          {date(o.createdAt)} · {o.fulfilment}
                        </small>
                      </div>
                      <Status value={o.status} />
                    </div>
                    {admin && (
                      <p>
                        <strong>{o.customerName}</strong> ·{" "}
                        <a href={"tel:" + o.customerPhone}>{o.customerPhone}</a>
                      </p>
                    )}
                    <div className="orderitems">
                      {o.items.map((l) => (
                        <div key={l.id}>
                          <span>
                            {l.name} <small>{l.variant}</small>
                          </span>
                          <strong>
                            {l.quantity} {l.unit} · {money(l.amount)}
                          </strong>
                        </div>
                      ))}
                    </div>
                    <div className="total">Total: {money(o.total)}</div>
                    <OrderPayments order={o} admin={admin} accountId={user.id} busy={busy} run={run} refresh={refresh} />
                    {o.address && <p>Address: {o.address}</p>}
                    {o.customerNotes && (
                      <p>Customer notes: {o.customerNotes}</p>
                    )}
                    {o.shopNotes && (
                      <p className="shopnote">Shop update: {o.shopNotes}</p>
                    )}
                    <div className="actions no-print">
                      <button
                        className="btn outline"
                        onClick={() => {
                          setSelectedOrder(o);
                          setTimeout(() => window.print(), 100);
                        }}
                      >
                        Print order summary
                      </button>
                      <button className="btn gold" onClick={()=>{try{openBill(o);}catch(e){setError(e.message);}}}>Bill / Save PDF</button>
                      {whatsappLink(o) && (
                        <a
                          className="btn outline"
                          style={{ background: "#166534", borderColor: "#166534", color: "#fff" }}
                          href={whatsappLink(o)}
                          target="_blank"
                          rel="noopener noreferrer"
                          title="WhatsApp उघडल्यावर message तपासा आणि Send दाबा"
                        >
                          {admin ? "WhatsApp customerला update पाठवा" : "WhatsApp दुकानाला order पाठवा"}
                        </a>
                      )}
                      {!admin && o.status === "PENDING" && (
                        <button
                          className="btn danger"
                          disabled={busy}
                          onClick={async () => {
                            if (confirm("Cancel this pending order?")) {
                              await run(
                                () => api.post("/orders/" + o.id + "/cancel"),
                                "Order cancelled",
                              );
                              await refresh();
                            }
                          }}
                        >
                          Cancel order
                        </button>
                      )}
                    </div>
                    {admin && nextStatuses(o).length > 0 && (
                      <div className="adminstatus no-print">
                        <Field label="Shop note to customer">
                          <input
                            maxLength={500}
                            value={orderNotes[o.id] || ""}
                            onChange={(e) =>
                              setOrderNotes({
                                ...orderNotes,
                                [o.id]: e.target.value,
                              })
                            }
                          />
                        </Field>
                        <div className="actions">
                          {nextStatuses(o).map((s) => (
                            <button
                              className={
                                "btn " +
                                (["REJECTED", "CANCELLED"].includes(s)
                                  ? "danger"
                                  : "gold")
                              }
                              key={s}
                              disabled={busy || (s === "CANCELLED" && Number(o.payment?.paid) > 0)}
                              title={s === "CANCELLED" && Number(o.payment?.paid) > 0 ? "Paid orders cannot be cancelled; refunds are not implemented" : undefined}
                              onClick={() => updateStatus(o, s)}
                            >
                              {s === "CONFIRMED"
                                ? "Confirm / order book करा"
                                : s}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </section>
                ))}
              {!orders.filter(matchesOrderFilter)
                .length && <div className="empty panel">{orderSearch.trim() || statusFilter || orderFromDate || orderToDate ? "No matching orders. Search, dates किंवा status filter बदला." : "No orders yet."}</div>}
            </>
          )}
          {page === "Payments" && user && <PaymentLedger orders={orders} admin={admin} onOpenOrders={()=>nav("Orders")} />}
          {page === "Notifications" && (
            <section className="panel">
              <div className="sectiontitle">
                <h2>Notifications</h2>
                <span className="count">{unread} unread</span>
              </div>
              <div className="notificationtools">
                <button
                  className="btn gold"
                  disabled={busy}
                  onClick={enableNotifications}
                >
                  Enable browser notifications
                </button>
                <button
                  className="btn outline"
                  onClick={async () => {
                    if (sound) {
                      setSound(false);
                      return;
                    }
                    try {
                      await playLoudAlert();
                      setSound(true);
                      setError("");
                    } catch {
                      setError("Sound सुरू झाला नाही. Browser आणि speaker volume तपासा.");
                    }
                  }}
                >
                  {sound ? "Sound on ✓" : "Enable alert sound"}
                </button>
              </div>
              {pushStatus && <p className="muted">{pushStatus}</p>}
              <p className="muted">
                App inbox काम करते. App बंद असताना push साठी HTTPS hosting,
                VAPID keys आणि browser permission आवश्यक आहेत.
              </p>
              {inbox.map((n) => (
                <article
                  className={"notification " + (n.read ? "" : "unread")}
                  key={n.id}
                >
                  <div>
                    <strong>{n.title}</strong>
                    <p>{n.body}</p>
                    <small>{date(n.createdAt)}</small>
                  </div>
                  <div className="actions">
                    <button
                      className="textbtn"
                      onClick={async () => {
                        if (!n.read)
                          await run(() =>
                            api.patch("/notifications/" + n.id + "/read"),
                          );
                        await refresh();
                        nav("Orders");
                      }}
                    >
                      View orders →
                    </button>
                    {!n.read && (
                      <button
                        className="textbtn"
                        disabled={busy}
                        onClick={async () => {
                          await run(() =>
                            api.patch("/notifications/" + n.id + "/read"),
                          );
                          await refresh();
                        }}
                      >
                        Mark read
                      </button>
                    )}
                  </div>
                </article>
              ))}
              {!inbox.length && (
                <div className="empty">No notifications yet.</div>
              )}
            </section>
          )}
          {admin && page === "Dashboard" && (
            <>
              <section className="adminhero">
                <span className="pill">SHOP CONTROL CENTRE</span>
                <h2>तुमचे दुकान. एकाच ठिकाणी.</h2>
                <p>Orders confirm करा, purchases नोंदवा आणि stock पाहा.</p>
                <button className="btn gold" onClick={() => nav("Orders")}>
                  Review orders →
                </button>
              </section>
              <div className="stats">
                {[
                  ["Outstanding / बाकी", money(outstandingAdminCents / 100)],
                  ["Orders with balance", outstandingAdminOrders.length],
                  ["Today’s orders", todayAdminOrders.length],
                  ["Today’s order sales", money(todayAdminSales)],
                  ["Pending orders", dashboard.pending || 0],
                  ["Low-stock products", dashboard.lowStock?.length || 0],
                  ["In progress", dashboard.active || 0],
                  ["Delivered", dashboard.delivered || 0],
                  ["Delivered sales", money(dashboard.sales)],
                  ["Purchase value", money(dashboard.purchaseValue)],
                ].map(([k, v]) => (
                  <section
                    className="stat"
                    key={k}
                    role="button"
                    tabIndex={0}
                    aria-label={`Open ${k}`}
                    style={{ cursor: "pointer" }}
                    onClick={() => openDashboardCard(k)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        openDashboardCard(k);
                      }
                    }}
                  >
                    <small>{k}</small>
                    <strong>{v}</strong>
                  </section>
                ))}
              </div>
              <p className="muted">Today’s order sales: आज तयार झालेल्या व Delivered झालेल्या ordersची एकूण रक्कम (भारत वेळ). ही payment receivedची रक्कम नाही.</p>
              <section className="panel" id="low-stock-alerts">
                <h3>Low stock alerts</h3>
                <div className="tablewrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>Available</th>
                        <th>Reserved</th>
                        <th>Alert level</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(dashboard.lowStock || []).map((p) => (
                        <tr key={p.id}>
                          <td>
                            {p.name} · {p.variant}
                          </td>
                          <td>
                            {p.available} {p.unit}
                          </td>
                          <td>{p.reserved}</td>
                          <td>{p.lowStock}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {!dashboard.lowStock?.length && (
                  <p className="muted">No low-stock products.</p>
                )}
              </section>
            </>
          )}
          {admin && page === "Products" && (
            <>
              <section className="panel">
                <h2>{editing ? "Edit product" : "Add actual product"}</h2>
                <form onSubmit={saveProduct}>
                  <div className="formrow">
                    {[
                      ["SKU", "sku"],
                      ["Product name", "name"],
                      ["Variant / size / shade", "variant"],
                      ["Unit (box, tin, bag…)", "unit"],
                    ].map(([l, k]) => (
                      <Field label={l} key={k}>
                        <input
                          required={k !== "variant"}
                          maxLength={k === "sku" ? 50 : k === "unit" ? 40 : 150}
                          value={product[k]}
                          onChange={(e) =>
                            setProduct({ ...product, [k]: e.target.value })
                          }
                        />
                      </Field>
                    ))}
                  </div>
                  <div className="formrow">
                    <Field label="Category">
                      <select
                        value={product.category}
                        onChange={(e) =>
                          setProduct({ ...product, category: e.target.value })
                        }
                      >
                        {config.categories.map((k) => (
                          <option value={k} key={k}>
                            {names[k]}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Selling price ₹ / unit">
                      <input
                        type="number"
                        required
                        min="0.01"
                        step="0.01"
                        value={product.price}
                        onChange={(e) =>
                          setProduct({ ...product, price: e.target.value })
                        }
                      />
                    </Field>
                    <Field label="Low-stock threshold">
                      <input
                        type="number"
                        required
                        min="0"
                        max="1000000"
                        value={product.lowStock}
                        onChange={(e) =>
                          setProduct({
                            ...product,
                            lowStock: Number(e.target.value),
                          })
                        }
                      />
                    </Field>
                  </div>
                  <Field label="Product photo (JPG / PNG, maximum 2 MB)">
                    <input key={photoInputKey} type="file" accept="image/jpeg,image/png" disabled={busy}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (!file) { setProductPhoto(null); return; }
                        if (!["image/jpeg", "image/png"].includes(file.type) || file.size > 2 * 1024 * 1024) {
                          setError("JPG किंवा PNG photo निवडा. कमाल size 2 MB."); e.target.value = ""; setProductPhoto(null); return;
                        }
                        setError(""); setProductPhoto(file);
                      }} />
                    <SelectedPhotoPreview file={productPhoto} />
                    {!productPhoto && editing && products.find((p) => p.id === editing) && (
                      <div style={{width:140, marginTop:12}}><ProductPhoto compact product={products.find((p) => p.id === editing)} /></div>
                    )}
                    <p className="muted">Photo निवडा आणि Save product दाबा. Photo न निवडल्यास जुना photo कायम राहील.</p>
                  </Field>
                  <Field label="Description">
                    <textarea
                      rows="2"
                      maxLength={1000}
                      value={product.description}
                      onChange={(e) =>
                        setProduct({ ...product, description: e.target.value })
                      }
                    />
                  </Field>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={product.active}
                      onChange={(e) =>
                        setProduct({ ...product, active: e.target.checked })
                      }
                    />
                    Available in customer catalogue
                  </label>
                  <p className="muted">
                    Stock थेट edit होत नाही. Purchases मध्ये received quantity
                    नोंदवा.
                  </p>
                  <div className="actions">
                    <button className="btn gold" disabled={busy}>
                      Save product
                    </button>
                    {editing && (
                      <button
                        type="button"
                        className="btn outline"
                        onClick={() => {
                          setEditing(null);
                          setProductPhoto(null); setPhotoInputKey((k) => k + 1);
                          setProduct(blankProduct());
                        }}
                      >
                        Cancel edit
                      </button>
                    )}
                  </div>
                </form>
              </section>
              <section className="panel">
                <h3>Product & stock register</h3>
                <input
                  placeholder="Search name or SKU…"
                  aria-label="Search product register"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                <div className="tablewrap">
                  <table>
                    <thead>
                      <tr>
                        <th>SKU / product</th>
                        <th>Price</th>
                        <th>On hand</th>
                        <th>Reserved</th>
                        <th>Available</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {products
                        .filter((p) =>
                          (p.name + " " + p.sku)
                            .toLowerCase()
                            .includes(search.toLowerCase()),
                        )
                        .map((p) => (
                          <tr key={p.id}>
                            <td>
                              <div style={{width:80, marginBottom:8}}><ProductPhoto compact product={p} /></div>
                              <strong>{p.name}</strong>
                              <small>
                                {p.sku} · {p.variant} ·{" "}
                                {p.active ? "Active" : "Hidden"}
                              </small>
                            </td>
                            <td>
                              {money(p.price)} / {p.unit}
                            </td>
                            <td>{p.stock}</td>
                            <td>{p.reserved}</td>
                            <td>{p.available}</td>
                            <td>
                              <button
                                className="btn outline"
                                disabled={busy}
                                onClick={() => {
                                  setEditing(p.id);
                                  setProductPhoto(null); setPhotoInputKey((k) => k + 1);
                                  setProduct({
                                    sku: p.sku,
                                    name: p.name,
                                    category: p.category,
                                    variant: p.variant,
                                    unit: p.unit,
                                    description: p.description,
                                    price: p.price,
                                    lowStock: p.lowStock,
                                    active: p.active,
                                  });
                                  window.scrollTo({
                                    top: 0,
                                    behavior: "smooth",
                                  });
                                }}
                              >
                                Edit
                              </button>
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </section>
            </>
          )}
          {admin && page === "Purchases" && (
            <>
              <section className="panel">
                <h2>Receive purchase / stock inward</h2>
                <p className="muted">
                  Supplier कडून माल प्रत्यक्ष मिळाल्यावर नोंद करा. Save केल्यावर
                  stock वाढतो.
                </p>
                <form onSubmit={receivePurchase}>
                  <fieldset disabled={busy || !!pendingPurchase.current}>
                    <div className="formrow">
                      <Field label="Product">
                        <select
                          required
                          value={purchase.productId}
                          onChange={(e) =>
                            setPurchase({
                              ...purchase,
                              productId: e.target.value,
                            })
                          }
                        >
                          <option value="">Select product</option>
                          {products.map((p) => (
                            <option key={p.id} value={p.id}>
                              {p.name} · {p.variant} · {p.unit}
                            </option>
                          ))}
                        </select>
                      </Field>
                      <Field label="Supplier">
                        <input
                          required
                          maxLength={150}
                          value={purchase.supplier}
                          onChange={(e) =>
                            setPurchase({
                              ...purchase,
                              supplier: e.target.value,
                            })
                          }
                        />
                      </Field>
                      <Field label="Supplier invoice reference">
                        <input
                          required
                          maxLength={100}
                          value={purchase.invoiceReference}
                          onChange={(e) =>
                            setPurchase({
                              ...purchase,
                              invoiceReference: e.target.value,
                            })
                          }
                        />
                      </Field>
                    </div>
                    <div className="formrow">
                      <Field label="Received quantity">
                        <input
                          required
                          type="number"
                          min="1"
                          max="100000"
                          value={purchase.quantity}
                          onChange={(e) =>
                            setPurchase({
                              ...purchase,
                              quantity: Number(e.target.value),
                            })
                          }
                        />
                      </Field>
                      <Field label="Purchase cost ₹ / unit">
                        <input
                          required
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={purchase.unitCost}
                          onChange={(e) =>
                            setPurchase({
                              ...purchase,
                              unitCost: e.target.value,
                            })
                          }
                        />
                      </Field>
                      <Field label="Received date">
                        <input
                          required
                          type="date"
                          max={today()}
                          value={purchase.receivedDate}
                          onChange={(e) =>
                            setPurchase({
                              ...purchase,
                              receivedDate: e.target.value,
                            })
                          }
                        />
                      </Field>
                    </div>
                  </fieldset>
                  <p>
                    Total:{" "}
                    <strong>
                      {money(
                        Number(purchase.quantity) * Number(purchase.unitCost),
                      )}
                    </strong>
                  </p>
                  <button className="btn gold" disabled={busy}>
                    {pendingPurchase.current
                      ? "Retry same purchase"
                      : "Receive & update stock"}
                  </button>
                </form>
                {pendingPurchase.current && (
                  <p className="muted">
                    If the result is uncertain, check purchase history before
                    retrying. The same request ID is reused.
                  </p>
                )}
              </section>
              <section className="panel">
                <h3>Purchase history</h3>
                <div className="tablewrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Product</th>
                        <th>Supplier / reference</th>
                        <th>Quantity</th>
                        <th>Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {purchases.map((p) => (
                        <tr key={p.id}>
                          <td>{p.receivedDate}</td>
                          <td>{p.productName}</td>
                          <td>
                            {p.supplier}
                            <small>{p.invoiceReference}</small>
                          </td>
                          <td>{p.quantity}</td>
                          <td>{money(p.total)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            </>
          )}
          <footer className="no-print">
            ज्योतिर्लिंग ट्रेडर्स, गारगोटी · 8381017384 · गुणवत्ता • विश्वास •
            सेवा
          </footer>
        </main>
      </div>
      {authMode && (
        <div className="modalback no-print">
          <section
            className="authmodal"
            role="dialog"
            aria-modal="true"
            aria-label="Sign in"
          >
            <button
              className="close"
              aria-label="Close"
              onClick={() => setAuthMode(null)}
            >
              ×
            </button>
            <img className="monogram shop-brand-logo" src="/shop-logo.jpeg" alt="ज्योतिर्लिंग ट्रेडर्स" />
            <h2>
              {authMode === "register" ? "Customer नोंदणी" : "Welcome back"}
            </h2>
            <p>Customer किंवा दुकानाच्या admin account ने login करा.</p>
            <form onSubmit={authenticate}>
              {authMode === "register" && (
                <>
                  <Field label="Full name">
                    <input
                      autoComplete="name"
                      required
                      maxLength={100}
                      value={credentials.name}
                      onChange={(e) =>
                        setCredentials({ ...credentials, name: e.target.value })
                      }
                    />
                  </Field>
                  <Field label="Mobile number">
                    <input
                      required
                      autoComplete="tel"
                      pattern="[0-9]{10}"
                      maxLength={10}
                      value={credentials.phone}
                      onChange={(e) =>
                        setCredentials({
                          ...credentials,
                          phone: e.target.value,
                        })
                      }
                    />
                  </Field>
                </>
              )}
              <Field label="Email">
                <input
                  type="email"
                  autoComplete="username"
                  required
                  value={credentials.email}
                  onChange={(e) =>
                    setCredentials({ ...credentials, email: e.target.value })
                  }
                />
              </Field>
              <Field label="Password">
                <input
                  type="password"
                  autoComplete={
                    authMode === "register"
                      ? "new-password"
                      : "current-password"
                  }
                  required
                  minLength={authMode === "register" ? 10 : 1}
                  maxLength={128}
                  value={credentials.password}
                  onChange={(e) =>
                    setCredentials({ ...credentials, password: e.target.value })
                  }
                />
              </Field>
              {error && <p className="autherror">{error}</p>}
              <button className="btn gold full" disabled={busy}>
                {authMode === "register"
                  ? "Create customer account"
                  : "Sign in"}
              </button>
            </form>
            <button
              className="textbtn full"
              onClick={() => {
                setAuthMode(authMode === "register" ? "login" : "register");
                setError("");
              }}
            >
              {authMode === "register"
                ? "Already registered? Sign in"
                : "New customer? Register here"}
            </button>
          </section>
        </div>
      )}
      {selectedOrder && (
        <section className="printonly">
          <h1>ज्योतिर्लिंग ट्रेडर्स, गारगोटी</h1>
          <p>8381017384</p>
          <h2>Order summary #{selectedOrder.id}</h2>
          <p>
            {selectedOrder.customerName} · {selectedOrder.customerPhone}
          </p>
          <p>
            {date(selectedOrder.createdAt)} · {selectedOrder.status} ·{" "}
            {selectedOrder.fulfilment}
          </p>
          <p>{selectedOrder.address}</p>
          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th>Quantity</th>
                <th>Price</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {selectedOrder.items.map((l) => (
                <tr key={l.id}>
                  <td>
                    {l.name} · {l.variant}
                  </td>
                  <td>
                    {l.quantity} {l.unit}
                  </td>
                  <td>{money(l.price)}</td>
                  <td>{money(l.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <h3>Total: {money(selectedOrder.total)}</h3>
          <p>Paid: {money(selectedOrder.payment?.paid)} · Balance: {money(selectedOrder.payment?.balance)} · {selectedOrder.payment?.status}</p>
          <p>{selectedOrder.shopNotes}</p>
          <p>Order summary only. Not a tax invoice or proof of payment.</p>
        </section>
      )}
    </div>
  );
}
