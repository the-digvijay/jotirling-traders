import axios from "axios";
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8082/api",
  withCredentials: true,
  headers: { "X-App-Request": "1" },
  timeout: 20000,
});
export const money = (n) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(
    n || 0,
  );
export const today = () =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
export const errorText = (e) =>
  e.response?.data?.message ||
  (!e.isAxiosError ? e.message : null) ||
  "Connection failed. Check backend on port 8082; refresh before retrying.";
